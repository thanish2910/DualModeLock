package com.duallock

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

/**
 * Optional convenience barrier. The user must explicitly enable this service in Android Settings.
 * It observes foreground package changes only; it does not inspect message text or window content.
 * This is not a system-level security boundary and cannot guarantee every entry path is blocked.
 */
class WhatsAppWatchService : AccessibilityService() {
    private var lastPromptAt = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName != "com.whatsapp" && packageName != "com.whatsapp.w4b") return

        val store = PinStore(this)
        if (!store.isConfigured() || store.isMainLaunchAuthorized()) return

        val now = System.currentTimeMillis()
        if (now - lastPromptAt < 1500L) return
        lastPromptAt = now

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        try {
            startActivity(intent)
        } catch (_: SecurityException) {
            // Android may restrict activity launches from the background on some devices.
        }
    }

    override fun onInterrupt() = Unit
}
