package com.example.applist.ui

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.concurrent.atomic.AtomicBoolean

class ClearRecentsAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AppListAccessibility"
        private var instance: ClearRecentsAccessibilityService? = null
        private val isClearing = AtomicBoolean(false)

        /** Returns true if the service is currently running. */
        val isRunning: Boolean get() = instance != null

        /** Opens the system recents screen. Returns true on success. */
        fun openRecents(): Boolean {
            return instance?.performGlobalAction(GLOBAL_ACTION_RECENTS) ?: false
        }

        /**
         * Clears all recent apps by opening recents and clicking "Clear all".
         * Returns true if the action was initiated.
         */
        fun clearAllRecents(): Boolean {
            val svc = instance ?: return false
            if (isClearing.getAndSet(true)) return false // Already clearing
            svc.performGlobalAction(GLOBAL_ACTION_RECENTS)
            // Schedule the clear-all click after recents UI appears
            svc.handler?.postDelayed({ svc.findAndClickClearAll() }, 800)
            return true
        }
    }

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // No-op: service exists only to provide global actions (e.g. open recents, clear all)
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "Accessibility service connected")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        Log.d(TAG, "Accessibility service destroyed")
    }

    /**
     * Traverses the active window's node tree to find a "Clear all" / "Clear" / "Close all" button
     * and performs a click. OEMs use different text/content-desc; we match common patterns.
     */
    private fun findAndClickClearAll() {
        val root = rootInActiveWindow ?: return
        try {
            val clearButton = findClearButton(root)
            clearButton?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Log.d(TAG, "Clear all button clicked")
        } finally {
            isClearing.set(false)
            root.recycle()
        }
    }

    private fun findClearButton(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null

        // Check if this node looks like a "clear all" button
        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val className = node.className?.toString()?.lowercase() ?: ""

        val isCandidate = (text.contains("clear") || text.contains("close") || text.contains("remove") ||
                desc.contains("clear") || desc.contains("close") || desc.contains("remove")) &&
                (className.contains("button") || className.contains("imagebutton") || className.contains("textview"))

        if (isCandidate && node.isClickable) {
            return node
        }

        // Recurse into children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            val found = findClearButton(child)
            if (found != null) {
                child.recycle()
                return found
            }
            child?.recycle()
        }
        return null
    }
}