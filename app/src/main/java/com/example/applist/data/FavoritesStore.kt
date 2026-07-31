package com.example.applist.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Persists the set of favorited package names in SharedPreferences.
 */
class FavoritesStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("favorites", Context.MODE_PRIVATE)

    fun getAll(): Set<String> = prefs.getStringSet(KEY, emptySet()).orEmpty().toSet()

    /** Adds the package if absent, removes it if present. Returns the new set. */
    fun toggle(packageName: String): Set<String> {
        val current = getAll().toMutableSet()
        if (!current.add(packageName)) {
            current.remove(packageName)
        }
        prefs.edit().putStringSet(KEY, current).apply()
        return current
    }

    private companion object {
        const val KEY = "favorite_packages"
    }
}
