package com.example.applist.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.applist.data.AppDetails
import com.example.applist.model.AppInfo
import com.example.applist.util.ExportUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailsScreen(
    viewModel: AppListViewModel,
    packageName: String?,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val app = remember(state.apps, packageName) {
        state.apps.find { it.packageName == packageName }
    }

    if (app == null) {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("App details") }, navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                })
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("App not found")
            }
        }
        return
    }

    val repository = viewModel.repository
    val details by produceState<AppDetails?>(initialValue = null, key1 = packageName) {
        value = repository.getAppDetails(app.packageName)
    }
    val headerIcon by produceState<ImageBitmap?>(initialValue = null, key1 = packageName) {
        value = withContext(Dispatchers.IO) {
            repository.loadIcon(app.packageName)?.toBitmap()?.asImageBitmap()
        }
    }

    val context = LocalContext.current
    val launchIntent = remember(app.packageName) { repository.getLaunchIntent(app.packageName) }
    var isPinned by remember(app.packageName, state.pinned) {
        mutableStateOf(app.packageName in state.pinned)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(app.appName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.togglePinned(app.packageName)
                            isPinned = !isPinned
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Pin",
                            tint = if (isPinned) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item { AppHeader(app, headerIcon) }
            item {
                ActionButtons(
                    canLaunch = launchIntent != null,
                    onLaunch = { launchIntent?.let(context::startActivity) },
                    onSettings = { openAppSettings(context, app.packageName) },
                    onShare = { shareApp(context, app) }
                )
            }
            item { SectionTitle("Information") }
            item {
                InfoRow("Version", details?.appInfo?.versionName?.let {
                    "$it (${details?.appInfo?.versionCode})"
                } ?: app.versionName)
            }
            item {
                InfoRow(
                    "Installed",
                    if (app.installTime > 0) ExportUtils.formatTime(app.installTime) else "Unknown"
                )
            }
            item {
                InfoRow(
                    "Last update",
                    if (app.updateTime > 0) ExportUtils.formatTime(app.updateTime) else "Unknown"
                )
            }
            item { InfoRow("Size", ExportUtils.formatBytes(details?.sizeBytes ?: app.sizeBytes)) }
            item { InfoRow("UID", app.uid.toString()) }
            item { InfoRow("Target SDK", details?.targetSdk?.toString() ?: "Unknown") }
            item { InfoRow("Min SDK", details?.minSdk?.toString() ?: "Unknown") }
            item { InfoRow("Type", appTypeLabel(app)) }
            item { InfoRow("Status", if (app.isEnabled) "Enabled" else "Disabled") }
            if (!details?.sourceDir.isNullOrEmpty()) {
                item { InfoRow("APK path", details?.sourceDir.orEmpty()) }
            }
            if (!details?.dataDir.isNullOrEmpty()) {
                item { InfoRow("Data path", details?.dataDir.orEmpty()) }
            }
            val permissions = details?.permissions.orEmpty()
            if (permissions.isNotEmpty()) {
                item { SectionTitle("Permissions (${permissions.size})") }
                items(permissions) { permission ->
                    Text(
                        text = permission,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AppHeader(app: AppInfo, icon: ImageBitmap?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                text = app.appName,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActionButtons(
    canLaunch: Boolean,
    onLaunch: () -> Unit,
    onSettings: () -> Unit,
    onShare: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(onClick = onLaunch, enabled = canLaunch, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("Launch")
        }
        OutlinedButton(onClick = onSettings, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.Settings, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("Settings")
        }
        OutlinedButton(onClick = onShare, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.Share, contentDescription = null)
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp)
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(Modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(120.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun appTypeLabel(app: AppInfo): String = when {
    app.isUpdatedSystemApp -> "Updated system app"
    app.isSystemApp -> "System app"
    else -> "User app"
}

private fun openAppSettings(context: Context, packageName: String) {
    context.startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:$packageName")
        )
    )
}

private fun shareApp(context: Context, app: AppInfo) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, app.appName)
        putExtra(
            Intent.EXTRA_TEXT,
            "${app.appName} (${app.packageName}) v${app.versionName}"
        )
    }
    context.startActivity(Intent.createChooser(intent, "Share app"))
}
