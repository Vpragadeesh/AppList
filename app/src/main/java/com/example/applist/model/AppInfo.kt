package com.example.applist.model

import android.graphics.drawable.Drawable

/**
 * Data class representing information about an installed application.
 */
data class AppInfo(
    val appName: String,
    val packageName: String,
    val icon: Drawable?,
    val versionName: String,
    val versionCode: Long,
    val installTime: Long,
    val updateTime: Long,
    val isSystemApp: Boolean,
    val isUpdatedSystemApp: Boolean,
    val isEnabled: Boolean,
    val uid: Int,
    val sizeBytes: Long = -1L
)
