package com.example.applist.data

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.TrafficStats
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Per-sensor temperature reading (best-effort; may be empty). */
data class TempReading(val name: String, val celsius: Int)

/** One process-counted memory sample (best-effort). */
data class AppMemSample(val name: String, val memoryMb: Long)

/**
 * Snapshot of system-wide performance stats shown on the Monitor screen.
 */
data class SystemStats(
    val cpuUsagePercent: Int = 0,
    val cpuFreqGhz: Double = 0.0,
    val coreUsages: List<Int> = emptyList(),
    val ramUsedPercent: Int = 0,
    val ramUsedGb: Double = 0.0,
    val ramTotalGb: Double = 0.0,
    val gpuUsagePercent: Int? = null,
    val cpuTempCelsius: Int? = null,
    val temps: List<TempReading> = emptyList(),
    val rxKbps: Double = 0.0,
    val txKbps: Double = 0.0,
    val rxTotalMb: Double = 0.0,
    val txTotalMb: Double = 0.0,
    val storageUsedGb: Double = 0.0,
    val storageTotalGb: Double = 0.0,
    val batteryPercent: Int = -1,
    val batteryTempCelsius: Int = -1,
    val batteryCharging: Boolean = false,
    val appMem: List<AppMemSample> = emptyList()
)

/**
 * Rolling time-series buffers (newest last, max 60 samples ≈ last minute at 1s).
 */
data class MonitorHistory(
    val cpu: List<Int> = emptyList(),
    val ram: List<Int> = emptyList(),
    val rx: List<Double> = emptyList(),
    val tx: List<Double> = emptyList()
)

/**
 * Reads system performance data from /proc, /sys and framework APIs.
 * All access is best-effort: unavailable metrics degrade to safe defaults
 * instead of throwing.
 */
object SystemMonitor {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitorJob: Job? = null
    private var pollCount = 0L

    // CPU baselines (total, idle) — reset when monitoring stops so the next
    // session's first sample re-primes instead of spanning the pause.
    private var cpuPrev: Pair<Long, Long>? = null
    private var perCorePrev: List<Pair<Long, Long>>? = null

    // Network byte counters for rate calculation
    private var netPrev: Triple<Long, Long, Long>? = null // (rx, tx, elapsedMs)

    private val _stats = MutableStateFlow(SystemStats())
    val stats: StateFlow<SystemStats> = _stats.asStateFlow()

    private val _history = MutableStateFlow(MonitorHistory())
    val history: StateFlow<MonitorHistory> = _history.asStateFlow()

    /**
     * Starts polling system stats every [intervalMs] and emitting into [stats].
     * Calling repeatedly with a running monitor is a no-op.
     */
    fun startMonitoring(context: Context, intervalMs: Long = 1000) {
        if (monitorJob?.isActive == true) return
        monitorJob = scope.launch {
            while (isActive) {
                readStats(context)
                delay(intervalMs)
            }
        }
    }

    fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
        cpuPrev = null
        perCorePrev = null
        netPrev = null
    }

    /** Reads one fresh snapshot and publishes it into [stats] and [history]. */
    suspend fun readStats(context: Context): SystemStats = withContext(Dispatchers.IO) {
        pollCount++
        val lines = try { File("/proc/stat").readLines() } catch (e: Exception) { emptyList() }

        val ram = readRamInfo(context)
        val net = readNetwork()
        val storage = readStorage()
        val battery = batteryInfo(context)
        val snapshot = SystemStats(
            cpuUsagePercent = readCpuUsage(lines),
            cpuFreqGhz = readCpuFreqGhz(),
            coreUsages = readCoreUsages(lines),
            ramUsedPercent = ram.first,
            ramUsedGb = ram.second,
            ramTotalGb = ram.third,
            gpuUsagePercent = readGpuUsage(),
            cpuTempCelsius = readCpuTemp(),
            temps = readTemps(),
            rxKbps = net.rxKbps,
            txKbps = net.txKbps,
            rxTotalMb = net.rxTotalMb,
            txTotalMb = net.txTotalMb,
            storageUsedGb = storage.first,
            storageTotalGb = storage.second,
            batteryPercent = battery.first,
            batteryTempCelsius = battery.second,
            batteryCharging = battery.third,
            appMem = if (pollCount % 5 == 0L) readTopProcesses(context) else _stats.value.appMem
        )
        _stats.value = snapshot

        _history.value = _history.value.copy(
            cpu = append(_history.value.cpu, snapshot.cpuUsagePercent),
            ram = append(_history.value.ram, snapshot.ramUsedPercent),
            rx = appendD(_history.value.rx, snapshot.rxKbps),
            tx = appendD(_history.value.tx, snapshot.txKbps)
        )
        snapshot
    }

    private fun <T> append(list: List<T>, value: T): List<T> =
        (list + value).takeLast(HISTORY_MAX)

    private fun appendD(list: List<Double>, value: Double): List<Double> =
        (list + value).takeLast(HISTORY_MAX)

    // ── CPU ────────────────────────────────────────────────────────────────────

    /**
     * System-wide CPU usage from the aggregate "cpu" line of /proc/stat:
     * usage = 100 * (totalDelta - idleDelta) / totalDelta since the last call.
     */
    private fun readCpuUsage(lines: List<String>): Int {
        return try {
            val line = lines.firstOrNull { it.startsWith("cpu ") } ?: return 0
            val parts = line.split("\\s+".toRegex()).filter { it.isNotBlank() }
            val jiffies = parts.drop(1).take(8).map { it.toLong() }
            val idle = jiffies[3] + jiffies[4]              // idle + iowait
            val total = jiffies.sum()
            computePercent(total, idle, cpuPrev).also { cpuPrev = it.second }.first
        } catch (e: Exception) {
            0
        }
    }

    private fun computePercent(total: Long, idle: Long, prev: Pair<Long, Long>?): Pair<Int, Pair<Long, Long>> {
        return if (prev == null) 0 to (total to idle)
        else {
            val totalDelta = total - prev.first
            val idleDelta = idle - prev.second
            val pct = if (totalDelta <= 0) 0
            else ((totalDelta - idleDelta) * 100 / totalDelta).toInt().coerceIn(0, 100)
            pct to (total to idle)
        }
    }

    /**
     * Per-core usage parsed from cpu0..cpuN lines of /proc/stat.
     * Returns empty list until the second sample (baseline priming).
     */
    private fun readCoreUsages(lines: List<String>): List<Int> {
        return try {
            val coreLines = lines.filter { Regex("^cpu\\d+\\s").containsMatchIn(it) }
                .sortedBy { s -> Regex("\\d+").find(s)?.value?.toIntOrNull() ?: 0 }
            if (coreLines.isEmpty()) return emptyList()
            val current = coreLines.map { l ->
                val j = l.split("\\s+".toRegex()).filter { it.isNotBlank() }.drop(1).take(8).map { it.toLong() }
                (j.sum()) to (j[3] + j[4])
            }
            val prev = perCorePrev
            if (prev == null || prev.size != current.size) {
                perCorePrev = current
                current.indices.map { 0 }
            } else {
                current.mapIndexed { i, (total, idle) ->
                    val (pt, pi) = prev[i]
                    val td = total - pt
                    val idlet = idle - pi
                    if (td <= 0) 0 else ((td - idlet) * 100 / td).toInt().coerceIn(0, 100)
                }.also { perCorePrev = current }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Current CPU frequency in GHz across all online cores, best-effort. */
    private fun readCpuFreqGhz(): Double {
        return try {
            val freqKhz = (0 until Runtime.getRuntime().availableProcessors()).maxOfOrNull { cpu ->
                for (path in listOf(
                    "/sys/devices/system/cpu/cpu$cpu/cpufreq/scaling_cur_freq",
                    "/sys/devices/system/cpu/cpu$cpu/cpufreq/cpuinfo_cur_freq"
                )) {
                    val read = File(path).bufferedReader().readLine()?.trim()?.toLongOrNull()
                    if (read != null) return@maxOfOrNull read
                }
                0L
            } ?: 0L
            if (freqKhz > 0) freqKhz / 1_000_000.0 else 0.0
        } catch (e: Exception) {
            0.0
        }
    }

    // ── RAM ────────────────────────────────────────────────────────────────────

    private fun readRamInfo(context: Context): Triple<Int, Double, Double> {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val info = ActivityManager.MemoryInfo()
            am.getMemoryInfo(info)
            if (info.totalMem <= 0) Triple(0, 0.0, 0.0)
            else {
                val used = (info.totalMem - info.availMem).coerceAtLeast(0)
                Triple(
                    (used * 100 / info.totalMem).toInt().coerceIn(0, 100),
                    used / GB, info.totalMem / GB
                )
            }
        } catch (e: Exception) {
            Triple(0, 0.0, 0.0)
        }
    }

    // ── GPU ────────────────────────────────────────────────────────────────────

    // Common per-vendor sysfs paths exposing GPU busy percentage.
    private val gpuPaths = listOf(
        "/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage",   // Qualcomm Adreno
        "/sys/class/misc/mali0/device/utilisation"        // Some Mali kernels
    )

    private fun readGpuUsage(): Int? {
        for (path in gpuPaths) {
            try {
                val file = File(path)
                if (!file.exists()) continue
                val content = file.bufferedReader().readText().trim()
                val num = content.substringBefore('%').takeLast(3).trim().toIntOrNull()
                if (num != null && num in 0..100) return num
                val m = Regex("(\\d{1,3})\\s*%").find(content)?.groupValues?.get(1)?.toIntOrNull()
                if (m != null && m in 0..100) return m
            } catch (e: Exception) {
                // Try next candidate
            }
        }
        return null
    }

    // ── Temperature ────────────────────────────────────────────────────────────

    /** Primary CPU temperature in °C, best-effort. */
    private fun readCpuTemp(): Int? {
        val temps = readTemps()
        return (temps.firstOrNull { it.name.contains("cpu", ignoreCase = true) }
            ?: temps.firstOrNull())?.celsius
    }

    /** All readable thermal zones (name + °C), capped to 8 to keep the UI tidy. */
    private fun readTemps(): List<TempReading> {
        val out = ArrayList<TempReading>()
        return try {
            val dir = File("/sys/class/thermal")
            val zones = dir.listFiles { f -> f.name.startsWith("thermal_zone") }
                ?.sortedBy { it.name } ?: return emptyList()
            for (zone in zones) {
                if (out.size >= 8) break
                try {
                    val type = File(zone, "type").bufferedReader().readLine()?.trim() ?: continue
                    val raw = File(zone, "temp").bufferedReader().readLine()?.trim()?.toIntOrNull() ?: continue
                    val celsius = raw / 1000
                    if (celsius in -50..150) out.add(TempReading(type, celsius))
                } catch (e: Exception) {
                    // Zone unreadable — continue
                }
            }
            out
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ── Network ────────────────────────────────────────────────────────────────

    private data class NetSample(
        val rxKbps: Double, val txKbps: Double,
        val rxTotalMb: Double, val txTotalMb: Double
    )

    private fun readNetwork(): NetSample {
        return try {
            val rx = TrafficStats.getTotalRxBytes()
            val tx = TrafficStats.getTotalTxBytes()
            if (rx < 0 || tx < 0) return NetSample(0.0, 0.0, 0.0, 0.0)
            val now = System.currentTimeMillis()
            val prev = netPrev
            val (rxKbps, txKbps) = if (prev == null) {
                0.0 to 0.0
            } else {
                val dt = ((now - prev.third) / 1000.0).coerceAtLeast(0.001)
                ((rx - prev.first) / 1024.0 / dt) to ((tx - prev.second) / 1024.0 / dt)
            }
            netPrev = Triple(rx, tx, now)
            NetSample(
                rxKbps.coerceAtLeast(0.0), txKbps.coerceAtLeast(0.0),
                rx / 1_048_576.0, tx / 1_048_576.0
            )
        } catch (e: Exception) {
            NetSample(0.0, 0.0, 0.0, 0.0)
        }
    }

    // ── Storage ────────────────────────────────────────────────────────────────

    private fun readStorage(): Pair<Double, Double> {
        return try {
            val stat = StatFs(Environment.getDataDirectory().absolutePath)
            val total = stat.totalBytes
            val free = stat.availableBytes
            (total - free) / GB to total / GB
        } catch (e: Exception) {
            0.0 to 0.0
        }
    }

    // ── Battery ────────────────────────────────────────────────────────────────

    private fun batteryInfo(context: Context): Triple<Int, Int, Boolean> {
        return try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                ?: return Triple(-1, -1, false)
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val pct = if (level >= 0 && scale > 0) level * 100 / scale else -1
            val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE) / 10
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            Triple(pct, temp, status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL)
        } catch (e: Exception) {
            Triple(-1, -1, false)
        }
    }

    // ── Top processes by memory (best-effort) ──────────────────────────────────

    private fun readTopProcesses(context: Context): List<AppMemSample> {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val procs = am.runningAppProcesses ?: return emptyList()
            procs.take(40).mapNotNull { proc ->
                try {
                    val name = proc.processName.ifBlank { return@mapNotNull null }
                    val arr = am.getProcessMemoryInfo(intArrayOf(proc.pid))
                    if (arr.isNotEmpty()) {
                        AppMemSample(name, (arr[0].totalPss / 1024).coerceAtLeast(0).toLong())
                    } else null
                } catch (e: Exception) {
                    null
                }
            }.sortedByDescending { it.memoryMb }.take(8)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private const val HISTORY_MAX = 60
    private const val GB = 1024.0 * 1024.0 * 1024.0
}
