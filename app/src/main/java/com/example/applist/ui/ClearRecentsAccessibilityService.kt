package com.example.applist.ui

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi

class ClearRecentsAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "ClearRecentsService"
        // Common recents/launcher packages across OEMs
        private val RECENTS_PACKAGES = setOf(
            "com.android.systemui",
            "com.google.android.apps.nexuslauncher",
            "com.sec.android.app.launcher",
            "com.miui.home",
            "com.oppo.launcher",
            "com.vivo.launcher",
            "com.oneplus.launcher",
            "com.huawei.android.launcher",
            "com.asus.launcher",
            "com.motorola.launcher"
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {

            val packageName = event.packageName?.toString()
            if (packageName != null && RECENTS_PACKAGES.contains(packageName)) {
                Log.d(TAG, "Recents screen detected: $packageName")
                clearRecents()
            }
        }
    }

    private fun clearRecents() {
        val rootNode = rootInActiveWindow ?: return

        // Try multiple strategies to find "Clear all" button
        val clearButton = findClearAllButton(rootNode)
        clearButton?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        clearButton?.recycle()
    }

    private fun findClearAllButton(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // Strategy 1: Find by text/content-description containing "clear"
        val clearTexts = listOf("clear all", "clear", "dismiss all", "remove all", "close all", "전체 삭제", "すべて消去", "清除全部")
        
        for (text in clearTexts) {
            val result = findNodeByText(node, text, ignoreCase = true)
            if (result != null) return result
        }

        // Strategy 2: Find by common view IDs
        val clearIds = listOf(
            "clear_all_button",
            "dismiss_all_button",
            "clear_button",
            "btn_clear_all",
            "clear_all"
        )
        for (id in clearIds) {
            val result = findNodeByViewId(node, id)
            if (result != null) return result
        }

        // Strategy 3: Find clickable buttons near bottom of recents screen (API 29+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return findClickableButtonNearBottom(node)
        }
        return null
    }

    private fun findNodeByText(node: AccessibilityNodeInfo, searchText: String, ignoreCase: Boolean): AccessibilityNodeInfo? {
        val text = node.text?.toString()
        if (text != null) {
            val match = if (ignoreCase) text.equals(searchText, ignoreCase = true) else text == searchText
            if (match && node.isClickable) return node
        }
        if (node.contentDescription != null) {
            val desc = node.contentDescription.toString()
            val match = if (ignoreCase) desc.contains(searchText, ignoreCase = true) else desc.contains(searchText)
            if (match && node.isClickable) return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeByText(child, searchText, ignoreCase)
            if (found != null) return found
            child.recycle()
        }
        return null
    }

    private fun findNodeByViewId(node: AccessibilityNodeInfo, viewId: String): AccessibilityNodeInfo? {
        val id = node.viewIdResourceName
        if (id != null && id.endsWith(viewId) && node.isClickable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeByViewId(child, viewId)
            if (found != null) return found
            child.recycle()
        }
        return null
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun findClickableButtonNearBottom(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // boundsInScreen requires API 29+
        val bounds = android.graphics.Rect()
        node.getBoundsInScreen(bounds)
        val screenHeight = bounds.bottom
        val threshold = (screenHeight * 0.75).toInt() // Bottom 25% of screen
        
        var bestCandidate: AccessibilityNodeInfo? = null
        var bestY = 0
        
        fun traverse(n: AccessibilityNodeInfo) {
            val childBounds = android.graphics.Rect()
            n.getBoundsInScreen(childBounds)
            if (n.isClickable && childBounds.top > threshold) {
                val centerY = (childBounds.top + childBounds.bottom) / 2
                if (centerY > bestY) {
                    bestY = centerY
                    bestCandidate?.recycle()
                    bestCandidate = n
                }
            }
            for (i in 0 until n.childCount) {
                val child = n.getChild(i) ?: continue
                traverse(child)
                child.recycle()
            }
        }
        
        traverse(node)
        return bestCandidate
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility service connected")
    }
}