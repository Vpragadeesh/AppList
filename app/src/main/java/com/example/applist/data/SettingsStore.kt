package com.example.applist.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Persists user preferences that are not tied to a single app entry.
 */
class SettingsStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /**
     * Whether system apps should appear in the app list.
     * Defaults to false so a fresh install only shows user-installed apps.
     */
    fun shouldShowSystemApps(): Boolean = prefs.getBoolean(KEY_SHOW_SYSTEM_APPS, false)

    fun setShowSystemApps(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_SYSTEM_APPS, show).apply()
    }

    private companion object {
        const val KEY_SHOW_SYSTEM_APPS = "show_system_apps"
    }
}
