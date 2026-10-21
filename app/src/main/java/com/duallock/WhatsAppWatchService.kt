package com.duallock

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

/**
 * Optional app-lock helper. The user must explicitly enable this service in Android Settings.
 * It observes foreground package changes only; it does not inspect chat text or message content.
 *
 * This is a convenience barrier, not a system-level security boundary. Android may restrict
 * launching an Activity from the background and alternate entry points can still exist.
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
            action = MainActivity.ACTION_SHOW_LOCK
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
    }

    override fun onInterrupt() = Unit
}