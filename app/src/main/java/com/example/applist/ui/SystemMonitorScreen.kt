package com.example.applist.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.applist.data.SystemMonitor
import com.example.applist.ui.AccessibilitySettingsActivity
import kotlinx.coroutines.launch

/**
 * btop-style System Monitor dashboard.
 *
 * Layout responsiveness: panels are laid out in single column on narrow
 * screens (< 640dp), and in a two-column adaptive grid on phones in
 * landscape, tablets and foldables. Panel heights are fixed so both device
 * classes render identically, only grouping changes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemMonitorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val stats by SystemMonitor.stats.collectAsState()
    val history by SystemMonitor.history.collectAsState()

    // 1s live sampling while visible; stops on exit, history is preserved.
    DisposableEffect(Unit) {
        SystemMonitor.startMonitoring(context, intervalMs = 1000)
        onDispose { SystemMonitor.stopMonitoring() }
    }

    fun clearAllRecents() {
        if (ClearRecentsAccessibilityService.isRunning) {
            if (!ClearRecentsAccessibilityService.clearAllRecents()) {
                Toast.makeText(context, "Could not clear recents — try again", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Cleared recent apps", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(
                context,
                "Enable the AppList accessibility service to clear all recent apps",
                Toast.LENGTH_LONG
            ).show()
            val intent = Intent(context, AccessibilitySettingsActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    Scaffold(
        containerColor = MonitorTheme.Bg,
        topBar = {
            TopAppBar(
                title = { Text("System Monitor") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { clearAllRecents() }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear recent apps")
                    }
                    IconButton(onClick = { scope.launch { SystemMonitor.readStats(context) } }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MonitorTheme.Bg,
                    titleContentColor = MonitorTheme.Text,
                    navigationIconContentColor = MonitorTheme.Text,
                    actionIconContentColor = MonitorTheme.Text
                )
            )
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val twoColumn = maxWidth >= 640.dp
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 10.dp)
            ) {
                // CPU spans full width on both layouts
                CpuPanel(stats = stats, history = history)
                PanelGap()

                if (twoColumn) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MemoryPanel(
                            stats = stats, history = history,
                            modifier = Modifier.weight(1f)
                        )
                        TempsPanel(stats = stats, modifier = Modifier.weight(1f))
                    }
                    PanelGap()
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NetworkPanel(
                            stats = stats, history = history,
                            modifier = Modifier.weight(1f)
                        )
                        StoragePanel(stats = stats, modifier = Modifier.weight(1f))
                    }
                } else {
                    MemoryPanel(stats = stats, history = history)
                    PanelGap()
                    TempsPanel(stats = stats)
                    PanelGap()
                    NetworkPanel(stats = stats, history = history)
                    PanelGap()
                    StoragePanel(stats = stats)
                }

                PanelGap()
                if (twoColumn) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BatteryPanel(stats = stats, modifier = Modifier.weight(1f))
                        ProcessesPanel(stats = stats, modifier = Modifier.weight(1f))
                    }
                } else {
                    BatteryPanel(stats = stats)
                    PanelGap()
                    ProcessesPanel(stats = stats)
                }
            }
        }
    }
}

@Composable
private fun PanelGap() {
    androidx.compose.foundation.layout.Spacer(Modifier.padding(vertical = 6.dp))
}
