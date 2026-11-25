package com.example.applist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.applist.data.SystemStats

/**
 * Card displaying live CPU / GPU / RAM / frequency metrics inside Settings.
 */
@Composable
fun SystemMonitorCard(
    stats: SystemStats,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "System Monitor",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh stats")
                }
            }
            Spacer(Modifier.height(8.dp))

            MetricBar("CPU", usageLabel(stats.cpuUsagePercent), stats.cpuUsagePercent)
            Spacer(Modifier.height(10.dp))
            MetricRow("CPU Clock", if (stats.cpuFreqGhz > 0) "%.2f GHz".format(stats.cpuFreqGhz) else "N/A")
            Spacer(Modifier.height(10.dp))
            MetricRow(
                "CPU Temp",
                stats.cpuTempCelsius?.let { "$it°C" } ?: "N/A",
                valueColor = when {
                    (stats.cpuTempCelsius ?: 0) > 80 -> Color.Red
                    (stats.cpuTempCelsius ?: 0) > 60 -> Color(0xFFFFA000) // amber
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            Spacer(Modifier.height(10.dp))
            MetricBar(
                "RAM",
                usageLabel(stats.ramUsedPercent) +
                    " (" + "%.1f".format(stats.ramUsedGb) +
                    " / " + "%.1f".format(stats.ramTotalGb) + " GB)",
                stats.ramUsedPercent
            )
            Spacer(Modifier.height(10.dp))
            MetricBar(
                "GPU",
                stats.gpuUsagePercent?.let { usageLabel(it) } ?: "N/A",
                stats.gpuUsagePercent ?: 0
            )
        }
    }
}

private fun usageLabel(percent: Int): String = "$percent%"

@Composable
private fun MetricRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = valueColor)
    }
}

@Composable
private fun MetricBar(label: String, value: String, percent: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(64.dp)
        )
        LinearProgressIndicator(
            progress = percent / 100f,
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            color = when {
                percent > 80 -> Color.Red
                percent > 60 -> Color(0xFFFFA000) // amber
                else -> MaterialTheme.colorScheme.primary
            }
        )
        Spacer(Modifier.width(10.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(140.dp),
            maxLines = 1
        )
    }
}
