package com.example.applist.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.applist.data.MonitorHistory
import com.example.applist.data.SystemStats
import java.util.Locale

/**
 * Terminal-style palette and typography for the btop-like Monitor screen.
 */
object MonitorTheme {
    val Bg = Color(0xFF0A0C10)
    val Text = Color(0xFFE6E9EF)
    val Dim = Color(0xFF8A93A6)
    val Border = Color(0xFF2A3340)
    val Cpu = Color(0xFF9ECE6A)   // green
    val Ram = Color(0xFFBB9AF7)   // violet
    val Rx = Color(0xFF73DACA)    // teal
    val Tx = Color(0xFFE0AF68)    // amber
    val Info = Color(0xFFED7ABE)  // magenta (panel labels)
    val Warn = Color(0xFFE0AF68)  // amber
    val Hot = Color(0xFFF7768E)   // red
    val Font = FontFamily.Monospace

    val CorePalette = listOf(
        Cpu, Ram, Rx, Tx, Info, Color(0xFF7DCFFF), Color(0xFFFFC777), Hot
    )
}

private val GraphHeight = 110.dp

private fun fmt(value: Double, pattern: String): String =
    String.format(Locale.US, pattern, value)

/** Fraction bar used by storage/battery aggregates. */
@Composable
fun UsageBar(percent: Int, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(9.dp)
            .background(MonitorTheme.Dim.copy(alpha = 0.15f), RoundedCornerShape(2.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth((percent / 100f).coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(color, RoundedCornerShape(2.dp))
        )
    }
}

/**
 * Panel container: bordered box with a header label straddling the top border,
 * btop style. Content must be compact; the screen scrolls if needed.
 */
@Composable
fun TerminalPanel(
    title: String,
    accent: Color = MonitorTheme.Info,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MonitorTheme.Border, RoundedCornerShape(2.dp))
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 10.dp)
    ) {
        Text(
            text = title,
            fontFamily = MonitorTheme.Font,
            color = accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 6.dp, y = (-19).dp)
                .background(MonitorTheme.Bg)
                .padding(horizontal = 4.dp)
        )
        Column(content = content)
    }
}

/** Label/value header row inside a panel. */
@Composable
private fun PanelHeader(left: String, right: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            left,
            fontFamily = MonitorTheme.Font,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MonitorTheme.Text,
            maxLines = 1
        )
        Text(
            right,
            fontFamily = MonitorTheme.Font,
            fontSize = 12.sp,
            color = MonitorTheme.Dim
        )
    }
}

/** One metric row (label left, value right) with ellipsized label. */
@Composable
private fun MetricRow(label: String, value: String, valueColor: Color = MonitorTheme.Text) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontFamily = MonitorTheme.Font,
            fontSize = 12.sp,
            color = MonitorTheme.Dim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            value,
            fontFamily = MonitorTheme.Font,
            fontSize = 12.sp,
            color = valueColor
        )
    }
}

/** Live area graph for fixed 0-100% series, newest sample on the right. */
@Composable
fun PercentGraph(
    history: List<Int>,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(GraphHeight)
    ) {
        val maxPoints = 60
        val w = size.width
        val h = size.height
        if (history.size >= 2) {
            val stepX = w / (maxPoints - 1)
            val line = Path()
            history.forEachIndexed { i, v ->
                val x = i * stepX
                val y = h * (1f - (v / 100f).coerceIn(0f, 1f))
                if (i == 0) line.moveTo(x, y) else line.lineTo(x, y)
            }
            val fill = Path().apply {
                addPath(line)
                lineTo((history.size - 1) * stepX, h)
                lineTo(0f, h)
                close()
            }
            drawPath(fill, color.copy(alpha = 0.22f))
            drawPath(line, color, style = Stroke(width = 2.dp.toPx()))
        } else {
            drawLine(
                MonitorTheme.Dim.copy(alpha = 0.35f),
                start = Offset(0f, h),
                end = Offset(w, h),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}

/** Dual-series KB/s graph (RX + TX lines) with auto scaling. */
@Composable
fun NetGraph(
    rx: List<Double>,
    tx: List<Double>,
    rxColor: Color,
    txColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(GraphHeight)
    ) {
        val maxPoints = 60
        val w = size.width
        val h = size.height
        val peak = ((rx + tx).maxOrNull() ?: 0.0).coerceAtLeast(64.0)
        fun seriesPath(series: List<Double>): Path? {
            if (series.size < 2) return null
            val path = Path()
            series.forEachIndexed { i, v ->
                val x = i * (w / (maxPoints - 1))
                val y = h * (1f - (v / peak).toFloat().coerceIn(0f, 1f))
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            return path
        }
        seriesPath(tx)?.let { drawPath(it, txColor, style = Stroke(width = 2.dp.toPx())) }
        seriesPath(rx)?.let { drawPath(it, rxColor, style = Stroke(width = 2.dp.toPx())) }
    }
}

/** One per-core usage bar: label + fill bar + percent. */
@Composable
fun CoreUsageBar(
    label: String,
    percent: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontFamily = MonitorTheme.Font,
            fontSize = 11.sp,
            color = MonitorTheme.Dim,
            modifier = Modifier.width(44.dp),
            maxLines = 1
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(9.dp)
                .background(MonitorTheme.Dim.copy(alpha = 0.15f), RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((percent / 100f).coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(color, RoundedCornerShape(2.dp))
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            "$percent%",
            fontFamily = MonitorTheme.Font,
            fontSize = 11.sp,
            color = MonitorTheme.Text,
            textAlign = TextAlign.End,
            modifier = Modifier.width(40.dp)
        )
    }
}

// ── Panels ─────────────────────────────────────────────────────────────────────

@Composable
fun CpuPanel(stats: SystemStats, history: MonitorHistory, modifier: Modifier = Modifier) {
    TerminalPanel(title = "CPU", accent = MonitorTheme.Cpu, modifier = modifier) {
        PanelHeader(
            left = "CPU ${stats.cpuUsagePercent}%",
            right = if (stats.cpuFreqGhz > 0) "${fmt(stats.cpuFreqGhz, "%.2f")} GHz" else "0.00 GHz"
        )
        Spacer(Modifier.height(6.dp))
        PercentGraph(history.cpu, MonitorTheme.Cpu)
        Spacer(Modifier.height(10.dp))
        val indexed = stats.coreUsages.mapIndexed { i, p -> i to p }
        indexed.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { (index, percent) ->
                    CoreUsageBar(
                        label = "cpu$index",
                        percent = percent,
                        color = MonitorTheme.CorePalette[index % MonitorTheme.CorePalette.size],
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
fun MemoryPanel(stats: SystemStats, history: MonitorHistory, modifier: Modifier = Modifier) {
    TerminalPanel(title = "Memory", accent = MonitorTheme.Ram, modifier = modifier) {
        PanelHeader(
            left = "RAM ${stats.ramUsedPercent}%",
            right = "${fmt(stats.ramUsedGb, "%.1f")} / ${fmt(stats.ramTotalGb, "%.1f")} GB"
        )
        Spacer(Modifier.height(6.dp))
        PercentGraph(history.ram, MonitorTheme.Ram)
    }
}

@Composable
fun TempsPanel(stats: SystemStats, modifier: Modifier = Modifier) {
    TerminalPanel(title = "Temperatures", accent = MonitorTheme.Info, modifier = modifier) {
        if (stats.temps.isEmpty()) {
            Text(
                "No thermal zones readable on this device",
                fontFamily = MonitorTheme.Font,
                fontSize = 12.sp,
                color = MonitorTheme.Dim
            )
        } else {
            stats.temps.take(6).forEach { sensor ->
                MetricRow(
                    label = sensor.name,
                    value = "${sensor.celsius}°C",
                    valueColor = when {
                        sensor.celsius > 80 -> MonitorTheme.Hot
                        sensor.celsius > 60 -> MonitorTheme.Warn
                        else -> MonitorTheme.Text
                    }
                )
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun NetworkPanel(stats: SystemStats, history: MonitorHistory, modifier: Modifier = Modifier) {
    TerminalPanel(title = "Network", accent = MonitorTheme.Rx, modifier = modifier) {
        val rx = if (stats.rxKbps >= 1024) "${fmt(stats.rxKbps / 1024, "%.2f")} MB/s" else "${fmt(stats.rxKbps, "%.0f")} KB/s"
        val tx = if (stats.txKbps >= 1024) "${fmt(stats.txKbps / 1024, "%.2f")} MB/s" else "${fmt(stats.txKbps, "%.0f")} KB/s"
        PanelHeader(left = "RX: $rx", right = "TX: $tx")
        Spacer(Modifier.height(6.dp))
        NetGraph(history.rx, history.tx, MonitorTheme.Rx, MonitorTheme.Tx)
        Spacer(Modifier.height(8.dp))
        MetricRow("Total RX", "${fmt(stats.rxTotalMb, "%.1f")} MB (since boot)")
    }
}

@Composable
fun StoragePanel(stats: SystemStats, modifier: Modifier = Modifier) {
    val percent = if (stats.storageTotalGb > 0)
        (stats.storageUsedGb / stats.storageTotalGb * 100).toInt().coerceIn(0, 100) else 0
    TerminalPanel(title = "Storage", accent = MonitorTheme.Tx, modifier = modifier) {
        PanelHeader(
            left = "/data",
            right = if (stats.storageTotalGb > 0)
                "${fmt(stats.storageUsedGb, "%.1f")} / ${fmt(stats.storageTotalGb, "%.1f")} GB" else "N/A"
        )
        Spacer(Modifier.height(10.dp))
        UsageBar(percent, MonitorTheme.Tx)
        Spacer(Modifier.height(8.dp))
        MetricRow("State", "$percent% used")
    }
}

@Composable
fun BatteryPanel(stats: SystemStats, modifier: Modifier = Modifier) {
    val levelColor = when {
        stats.batteryPercent in 0..20 -> MonitorTheme.Hot
        stats.batteryPercent in 21..40 -> MonitorTheme.Warn
        else -> MonitorTheme.Cpu
    }
    TerminalPanel(title = "Battery", accent = MonitorTheme.Cpu, modifier = modifier) {
        if (stats.batteryPercent < 0) {
            Text(
                "Battery info unavailable",
                fontFamily = MonitorTheme.Font,
                fontSize = 12.sp,
                color = MonitorTheme.Dim
            )
        } else {
            PanelHeader(
                left = "${stats.batteryPercent}%",
                right = "${stats.batteryTempCelsius}°C · ${if (stats.batteryCharging) "charging" else "on battery"}"
            )
            Spacer(Modifier.height(10.dp))
            UsageBar(stats.batteryPercent, levelColor)
        }
    }
}

@Composable
fun ProcessesPanel(stats: SystemStats, modifier: Modifier = Modifier) {
    TerminalPanel(title = "Running apps (top memory)", accent = MonitorTheme.Hot, modifier = modifier) {
        if (stats.appMem.isEmpty()) {
            Text(
                "No process info available yet",
                fontFamily = MonitorTheme.Font,
                fontSize = 12.sp,
                color = MonitorTheme.Dim
            )
        } else {
            stats.appMem.take(8).forEach { sample ->
                MetricRow(
                    label = sample.name,
                    value = "${sample.memoryMb} MB"
                )
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
