package com.example.applist.util

import com.example.applist.model.AppCategory
import com.example.applist.model.AppInfo
import com.example.applist.model.SortOption

/**
 * Pure filtering and sorting logic, kept separate from the ViewModel so it can
 * be unit tested without an Android runtime.
 */
object AppFilter {

    fun apply(
        apps: List<AppInfo>,
        query: String,
        category: AppCategory,
        sortOption: SortOption,
        pinnedPackages: Set<String> = emptySet()
    ): List<AppInfo> {
        val q = query.trim()
        val filtered = apps.filter { app ->
            val categoryMatch = when (category) {
                AppCategory.ALL -> true
                AppCategory.USER -> !app.isSystemApp
                AppCategory.SYSTEM -> app.isSystemApp && !app.isUpdatedSystemApp
                AppCategory.UPDATED_SYSTEM -> app.isUpdatedSystemApp
                AppCategory.DISABLED -> !app.isEnabled
                AppCategory.PIN -> app.packageName in pinnedPackages
                AppCategory.RAM -> false
            }
            val queryMatch = q.isEmpty() ||
                app.appName.contains(q, ignoreCase = true) ||
                app.packageName.contains(q, ignoreCase = true)
            categoryMatch && queryMatch
        }
        return when (sortOption) {
            SortOption.NAME -> filtered.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.appName })
            SortOption.INSTALL_DATE -> filtered.sortedByDescending { it.installTime }
            SortOption.UPDATE_DATE -> filtered.sortedByDescending { it.updateTime }
            SortOption.SIZE -> filtered.sortedWith(
                compareByDescending<AppInfo> { it.sizeBytes >= 0 }
                    .thenByDescending { it.sizeBytes }
            )
            SortOption.USER_SYSTEM -> filtered.sortedWith(
                compareBy<AppInfo> { it.isSystemApp }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.appName }
            )
        }
    }
}
