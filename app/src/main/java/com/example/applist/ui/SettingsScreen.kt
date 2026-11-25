package com.example.applist.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.applist.ui.AccessibilitySettingsActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AppListViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    fun openRecentApps(ctx: Context) {
        if (ClearRecentsAccessibilityService.isRunning) {
            ClearRecentsAccessibilityService.openRecents()
        } else {
            Toast.makeText(
                ctx,
                "Enable the AppList accessibility service to use this feature",
                Toast.LENGTH_LONG
            ).show()
            val intent = Intent(ctx, AccessibilitySettingsActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
        }
    }

    fun clearAllRecents(ctx: Context) {
        if (ClearRecentsAccessibilityService.isRunning) {
            if (!ClearRecentsAccessibilityService.clearAllRecents()) {
                Toast.makeText(ctx, "Could not clear recents — try again", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(
                ctx,
                "Enable the AppList accessibility service to clear all recent apps",
                Toast.LENGTH_LONG
            ).show()
            val intent = Intent(ctx, AccessibilitySettingsActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { openRecentApps(context) }) {
                        Icon(Icons.Filled.Menu, contentDescription = "Open recent apps")
                    }
                    IconButton(onClick = { clearAllRecents(context) }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear all recent apps")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Hotspot card (Feature: open mobile hotspot settings)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Hotspot", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Open mobile hotspot settings", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { openHotspotSettings(context) }) {
                            Icon(Icons.Filled.ArrowForward, contentDescription = "Open hotspot settings", modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }

            // Bluetooth card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Bluetooth", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Open Bluetooth settings", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { openBluetoothSettings(context) }) {
                            Icon(Icons.Filled.ArrowForward, contentDescription = "Open Bluetooth settings", modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }

            // Show System Apps toggle
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Show System Apps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Include system apps in the app list", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = state.showSystemApps,
                            onCheckedChange = { viewModel.setShowSystemApps(it) }
                        )
                    }
                }
            }
        }
    }
}

private fun openBluetoothSettings(context: Context) {
    val candidates = listOf(
        Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS),
        Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS),
        Intent(android.provider.Settings.ACTION_SETTINGS)
    )
    val intent = candidates.firstOrNull { candidate ->
        candidate.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        candidate.resolveActivity(context.packageManager) != null
    }
    try {
        intent?.let { context.startActivity(it) }
    } catch (e: Exception) {
        try {
            context.startActivity(
                Intent(android.provider.Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (ignored: Exception) {
        }
    }
}

private fun openHotspotSettings(context: Context) {
    val pm = context.packageManager
    val candidates = listOf(
        Intent().setClassName("com.android.settings", "com.android.settings.TetherSettings"),
        Intent().setClassName("com.android.settings", "com.android.settings.Settings\$TetherSettingsActivity"),
        Intent("android.settings.TETHERING_SETTINGS"),
        Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS),
        Intent(android.provider.Settings.ACTION_WIFI_SETTINGS),
        Intent(android.provider.Settings.ACTION_SETTINGS)
    )
    val intent = candidates.firstOrNull { candidate ->
        candidate.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        candidate.resolveActivity(pm) != null
    }
    try {
        intent?.let { context.startActivity(it) }
    } catch (e: Exception) {
        try {
            context.startActivity(
                Intent(android.provider.Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (ignored: Exception) {
        }
    }
}
