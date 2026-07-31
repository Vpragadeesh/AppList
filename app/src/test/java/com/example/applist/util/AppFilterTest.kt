package com.example.applist.util

import com.example.applist.model.AppCategory
import com.example.applist.model.AppInfo
import com.example.applist.model.SortOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppFilterTest {

    private fun app(
        name: String,
        pkg: String,
        system: Boolean = false,
        updated: Boolean = false,
        enabled: Boolean = true,
        installTime: Long = 0L,
        updateTime: Long = 0L,
        sizeBytes: Long = -1L
    ) = AppInfo(
        appName = name,
        packageName = pkg,
        icon = null,
        versionName = "1.0",
        versionCode = 1,
        installTime = installTime,
        updateTime = updateTime,
        isSystemApp = system,
        isUpdatedSystemApp = updated,
        isEnabled = enabled,
        uid = 1000,
        sizeBytes = sizeBytes
    )

    @Test
    fun `filters by package name case insensitively`() {
        val apps = listOf(app("Phone", "com.android.phone"), app("Maps", "com.google.maps"))
        val result = AppFilter.apply(apps, "GOOGLE", AppCategory.ALL, SortOption.NAME)
        assertEquals(listOf("com.google.maps"), result.map { it.packageName })
    }

    @Test
    fun `filters by app name`() {
        val apps = listOf(app("Calculator", "com.a.calc"), app("Clock", "com.a.clock"))
        val result = AppFilter.apply(apps, "calc", AppCategory.ALL, SortOption.NAME)
        assertEquals(listOf("Calculator"), result.map { it.appName })
    }

    @Test
    fun `empty query returns everything`() {
        val apps = listOf(app("A", "a"), app("B", "b"))
        assertEquals(2, AppFilter.apply(apps, "", AppCategory.ALL, SortOption.NAME).size)
    }

    @Test
    fun `category user excludes system apps`() {
        val apps = listOf(app("User App", "u", system = false), app("Sys", "s", system = true))
        val result = AppFilter.apply(apps, "", AppCategory.USER, SortOption.NAME)
        assertEquals(listOf("u"), result.map { it.packageName })
    }

    @Test
    fun `category system excludes updated system apps`() {
        val apps = listOf(
            app("Plain", "p", system = true),
            app("Updated", "up", system = true, updated = true)
        )
        val result = AppFilter.apply(apps, "", AppCategory.SYSTEM, SortOption.NAME)
        assertEquals(listOf("p"), result.map { it.packageName })
    }

    @Test
    fun `category updated system only returns updated system apps`() {
        val apps = listOf(
            app("Updated", "up", system = true, updated = true),
            app("Plain", "p", system = true)
        )
        val result = AppFilter.apply(apps, "", AppCategory.UPDATED_SYSTEM, SortOption.NAME)
        assertEquals(listOf("up"), result.map { it.packageName })
    }

    @Test
    fun `category disabled returns disabled apps`() {
        val apps = listOf(app("On", "a", enabled = true), app("Off", "b", enabled = false))
        val result = AppFilter.apply(apps, "", AppCategory.DISABLED, SortOption.NAME)
        assertEquals(listOf("b"), result.map { it.packageName })
    }

    @Test
    fun `category pin returns only pinned apps`() {
        val apps = listOf(app("Pinned", "p", system = true), app("Other", "o"))
        val result = AppFilter.apply(
            apps, "", AppCategory.PIN, SortOption.NAME, pinnedPackages = setOf("p")
        )
        assertEquals(listOf("p"), result.map { it.packageName })
    }

    @Test
    fun `category pin with no pins returns empty`() {
        val apps = listOf(app("A", "a"))
        val result = AppFilter.apply(apps, "", AppCategory.PIN, SortOption.NAME)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `sorts by name case insensitively`() {
        val apps = listOf(app("banana", "b"), app("Apple", "a"))
        val result = AppFilter.apply(apps, "", AppCategory.ALL, SortOption.NAME)
        assertEquals(listOf("Apple", "banana"), result.map { it.appName })
    }

    @Test
    fun `sorts by install date descending`() {
        val apps = listOf(
            app("Old", "o", installTime = 100L),
            app("New", "n", installTime = 200L)
        )
        val result = AppFilter.apply(apps, "", AppCategory.ALL, SortOption.INSTALL_DATE)
        assertEquals(listOf("New", "Old"), result.map { it.appName })
    }

    @Test
    fun `sorts by size with unknown sizes last`() {
        val apps = listOf(
            app("Unknown", "u", sizeBytes = -1),
            app("Small", "s", sizeBytes = 100),
            app("Big", "b", sizeBytes = 900)
        )
        val result = AppFilter.apply(apps, "", AppCategory.ALL, SortOption.SIZE)
        assertEquals(listOf("Big", "Small", "Unknown"), result.map { it.appName })
    }

    @Test
    fun `sorts user apps before system apps`() {
        val apps = listOf(
            app("Sys B", "sb", system = true),
            app("User A", "ua", system = false),
            app("Sys A", "sa", system = true)
        )
        val result = AppFilter.apply(apps, "", AppCategory.ALL, SortOption.USER_SYSTEM)
        assertEquals(listOf("User A", "Sys A", "Sys B"), result.map { it.appName })
    }

    @Test
    fun `combines category and query`() {
        val apps = listOf(
            app("Settings", "com.android.settings", system = true),
            app("Settings X", "com.x.settings", system = false)
        )
        val result = AppFilter.apply(apps, "settings", AppCategory.USER, SortOption.NAME)
        assertEquals(listOf("com.x.settings"), result.map { it.packageName })
    }

    @Test
    fun `shows system apps in ALL category`() {
        val apps = listOf(
            app("Sys", "s", system = true),
            app("User", "u", system = false)
        )
        val result = AppFilter.apply(apps, "", AppCategory.ALL, SortOption.NAME)
        assertEquals(2, result.size)
    }

    @Test
    fun `does not mutate input list`() {
        val apps = listOf(app("B", "b"), app("A", "a"))
        val before = apps.map { it.packageName }
        AppFilter.apply(apps, "", AppCategory.ALL, SortOption.NAME)
        assertEquals(before, apps.map { it.packageName })
        assertTrue(apps.size == 2)
    }
}
