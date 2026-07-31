package com.example.applist.util

import com.example.applist.model.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportUtilsTest {

    private fun app(
        name: String,
        pkg: String,
        version: String = "1.0",
        versionCode: Long = 1,
        system: Boolean = false,
        uid: Int = 1000
    ) = AppInfo(
        appName = name,
        packageName = pkg,
        icon = null,
        versionName = version,
        versionCode = versionCode,
        installTime = 0L,
        updateTime = 0L,
        isSystemApp = system,
        isUpdatedSystemApp = false,
        isEnabled = true,
        uid = uid
    )

    @Test
    fun `csv has header and one row per app`() {
        val csv = ExportUtils.toCsv(listOf(app("Alpha", "com.a"), app("Beta", "com.b")))
        val lines = csv.trim().lines()
        assertEquals(3, lines.size)
        assertTrue(lines[0].startsWith("name,package"))
        assertTrue(lines[1].startsWith("Alpha,com.a"))
        assertTrue(lines[2].startsWith("Beta,com.b"))
    }

    @Test
    fun `csv escapes commas and quotes in names`() {
        val csv = ExportUtils.toCsv(listOf(app("A, B \"Special\"", "com.a")))
        val line = csv.trim().lines()[1]
        assertTrue(line.startsWith("\"A, B \"\"Special\"\"\""))
        assertTrue(line.contains(",com.a,"))
    }

    @Test
    fun `json is well formed`() {
        val json = ExportUtils.toJson(listOf(app("Alpha", "com.a", version = "2.3", versionCode = 4)))
        assertTrue(json.startsWith("{\"apps\":["))
        assertTrue(json.endsWith("]}"))
        assertTrue(json.contains("\"name\":\"Alpha\""))
        assertTrue(json.contains("\"package\":\"com.a\""))
        assertTrue(json.contains("\"version\":\"2.3\""))
        assertTrue(json.contains("\"versionCode\":4"))
    }

    @Test
    fun `json escapes quotes`() {
        val json = ExportUtils.toJson(listOf(app("Say \"Hi\"", "com.a")))
        assertTrue(json.contains("\"name\":\"Say \\\"Hi\\\"\""))
    }

    @Test
    fun `formatBytes handles units`() {
        assertEquals("Unknown", ExportUtils.formatBytes(-1))
        assertEquals("500 B", ExportUtils.formatBytes(500))
        assertEquals("1.00 KB", ExportUtils.formatBytes(1024))
        assertEquals("1.50 MB", ExportUtils.formatBytes(1572864))
        assertEquals("1.00 GB", ExportUtils.formatBytes(1073741824))
    }
}
