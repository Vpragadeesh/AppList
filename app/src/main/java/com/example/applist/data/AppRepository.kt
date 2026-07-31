package com.example.applist.data

import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.UserHandle
import android.os.storage.StorageManager
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap
import com.example.applist.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Detailed information about an app, loaded on demand for the details screen.
 */
data class AppDetails(
    val appInfo: AppInfo,
    val targetSdk: Int,
    val minSdk: Int,
    val sizeBytes: Long,
    val sourceDir: String,
    val dataDir: String,
    val permissions: List<String>,
    val hasLauncherActivity: Boolean
)

/**
 * Wraps PackageManager to load information about installed applications.
 */
class AppRepository(private val context: Context) {

    private val pm: PackageManager = context.packageManager

    /** Bounded in-memory cache of decoded app icons (12 MB of bitmaps). */
    private val iconCache = object : LruCache<String, Bitmap>(12 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    /**
     * Returns all installed packages (user + system) as [AppInfo].
     * Icons are NOT decoded here (they are the slow part); they are loaded
     * lazily per row via [loadIcon] so the initial list renders quickly.
     */
    suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val packages = pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0L))
        packages.mapNotNull { pkg ->
            val info = pkg.applicationInfo ?: return@mapNotNull null
            toAppInfo(pkg, info)
        }
    }

    /**
     * Loads and caches an app icon. Cheap for cached entries, decodes on
     * a background thread otherwise. Returns null if the package vanished.
     */
    suspend fun loadIcon(packageName: String): Drawable? = withContext(Dispatchers.IO) {
        runCatching {
            val cached = iconCache.get(packageName)
            if (cached != null) {
                BitmapDrawable(context.resources, cached)
            } else {
                pm.getApplicationIcon(packageName).also { drawable ->
                    runCatching { drawable.toBitmap().also { iconCache.put(packageName, it) } }
                }
            }
        }.getOrNull()
    }

    /** Loads extended details for a single package, or null if the app was uninstalled. */
    suspend fun getAppDetails(packageName: String): AppDetails? = withContext(Dispatchers.IO) {
        runCatching {
            val pkg = pm.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(
                    (PackageManager.GET_PERMISSIONS or PackageManager.GET_ACTIVITIES).toLong()
                )
            )
            val info = pkg.applicationInfo ?: return@runCatching null
            val base = toAppInfo(pkg, info)
            AppDetails(
                appInfo = base,
                targetSdk = info.targetSdkVersion,
                minSdk = info.minSdkVersion,
                sizeBytes = querySize(packageName, info.uid),
                sourceDir = info.sourceDir.orEmpty(),
                dataDir = info.dataDir.orEmpty(),
                permissions = pkg.requestedPermissions?.toList().orEmpty(),
                hasLauncherActivity = pm.getLaunchIntentForPackage(packageName) != null
            )
        }.getOrNull()
    }

    /** Returns a launch intent for the given package, or null if not launchable. */
    fun getLaunchIntent(packageName: String): Intent? = pm.getLaunchIntentForPackage(packageName)

    private fun toAppInfo(pkg: PackageInfo, info: ApplicationInfo): AppInfo {
        val systemFlag = info.flags and ApplicationInfo.FLAG_SYSTEM
        val updatedFlag = info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
        return AppInfo(
            appName = info.loadLabel(pm).toString(),
            packageName = pkg.packageName.orEmpty(),
            icon = null,
            versionName = pkg.versionName.orEmpty(),
            versionCode = pkg.longVersionCode,
            installTime = pkg.firstInstallTime,
            updateTime = pkg.lastUpdateTime,
            isSystemApp = systemFlag != 0,
            isUpdatedSystemApp = updatedFlag != 0,
            isEnabled = info.enabled,
            uid = info.uid
        )
    }

    /**
     * Best-effort storage size query. Returns -1 when unavailable
     * (e.g. missing PACKAGE_USAGE_STATS permission).
     */
    private fun querySize(packageName: String, uid: Int): Long {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val ssm = context.getSystemService(StorageStatsManager::class.java)
                val stats = ssm.queryStatsForPackage(
                    StorageManager.UUID_DEFAULT,
                    packageName,
                    UserHandle.getUserHandleForUid(uid)
                )
                stats.appBytes + stats.dataBytes + stats.cacheBytes
            } else {
                -1L
            }
        } catch (_: Exception) {
            -1L
        }
    }
}
