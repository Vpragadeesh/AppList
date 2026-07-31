package com.example.applist.util

import com.example.applist.model.AppInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds shareable text representations of the installed app list.
 */
object ExportUtils {

    fun toCsv(apps: List<AppInfo>): String = buildString {
        appendLine("name,package,version,versionCode,installTime,updateTime,isSystemApp,isUpdatedSystemApp,isEnabled,uid")
        apps.forEach { app ->
            appendLine(
                listOf(
                    csvField(app.appName),
                    app.packageName,
                    csvField(app.versionName),
                    app.versionCode,
                    app.installTime,
                    app.updateTime,
                    app.isSystemApp,
                    app.isUpdatedSystemApp,
                    app.isEnabled,
                    app.uid
                ).joinToString(",")
            )
        }
    }

    fun toJson(apps: List<AppInfo>): String = buildString {
        append("{\"apps\":[")
        apps.forEachIndexed { index, app ->
            if (index > 0) append(",")
            append("{")
            append("\"name\":\"${jsonField(app.appName)}\",")
            append("\"package\":\"${jsonField(app.packageName)}\",")
            append("\"version\":\"${jsonField(app.versionName)}\",")
            append("\"versionCode\":${app.versionCode},")
            append("\"installTime\":${app.installTime},")
            append("\"updateTime\":${app.updateTime},")
            append("\"isSystemApp\":${app.isSystemApp},")
            append("\"isUpdatedSystemApp\":${app.isUpdatedSystemApp},")
            append("\"isEnabled\":${app.isEnabled},")
            append("\"uid\":${app.uid}")
            append("}")
        }
        append("]}")
    }

    fun formatBytes(bytes: Long): String {
        if (bytes < 0) return "Unknown"
        val kb = 1024.0
        val mb = kb * 1024
        val gb = mb * 1024
        return when {
            bytes >= gb -> String.format(Locale.US, "%.2f GB", bytes / gb)
            bytes >= mb -> String.format(Locale.US, "%.2f MB", bytes / mb)
            bytes >= kb -> String.format(Locale.US, "%.2f KB", bytes / kb)
            else -> "$bytes B"
        }
    }

    fun formatTime(timestamp: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))

    private fun csvField(value: String): String {
        val needsQuotes = value.contains(',') || value.contains('"') || value.contains('\n')
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuotes) "\"$escaped\"" else escaped
    }

    private fun jsonField(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"")
}
