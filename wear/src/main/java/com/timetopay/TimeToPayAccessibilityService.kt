package com.timetopay

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class TimeToPayAccessibilityService : AccessibilityService() {
    private var isTargetAppForeground = false

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        val targets = TargetPackages.getSelected(this)
        if (targets.isEmpty()) {
            return
        }

        val packageName = event.packageName?.toString() ?: return

        if (packageName in targets) {
            if (!isTargetAppForeground) {
                NfcController.setNfcEnabled(this, true)
                isTargetAppForeground = true
            }
        } else if (isTargetAppForeground) {
            NfcController.setNfcEnabled(this, false)
            isTargetAppForeground = false
        }
    }

    override fun onInterrupt() {
        // Required lifecycle method.
    }
}
