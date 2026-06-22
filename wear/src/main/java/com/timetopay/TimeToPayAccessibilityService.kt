package com.timetopay

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.util.Log

class TimeToPayAccessibilityService : AccessibilityService() {
    private var isTargetAppForeground = false
    private var lastForegroundPackage: String? = null
    private var selectionListener: SharedPreferences.OnSharedPreferenceChangeListener? = null
    private var pendingRestartPackage: String? = null
    private var pendingRestartBypassCooldown = false
    private var lastRestartAtMs = 0L
    private val handler = Handler(Looper.getMainLooper())

    private val nfcStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != NfcAdapter.ACTION_ADAPTER_STATE_CHANGED) {
                return
            }
            val state = intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE, NfcAdapter.STATE_OFF)
            if (state == NfcAdapter.STATE_ON) {
                maybeRestartPendingTarget()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(nfcStateReceiver, IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED), RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(nfcStateReceiver, IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED))
        }
        selectionListener = TargetPackages.registerOnSelectionChanged(this) {
            reevaluateForeground(lastForegroundPackage)
        }
        rootInActiveWindow?.packageName?.toString()?.let { packageName ->
            if (!isTransientOverlay(packageName)) {
                lastForegroundPackage = packageName
                reevaluateForeground(packageName)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        val packageName = event.packageName?.toString() ?: return
        if (isTransientOverlay(packageName)) {
            return
        }
        lastForegroundPackage = packageName
        reevaluateForeground(packageName)
    }

    override fun onInterrupt() {
        // Required lifecycle method.
    }

    override fun onDestroy() {
        cancelPendingRestart()
        runCatching { unregisterReceiver(nfcStateReceiver) }
        selectionListener?.let { listener ->
            TargetPackages.unregisterOnSelectionChanged(this, listener)
        }
        selectionListener = null
        super.onDestroy()
    }

    private fun reevaluateForeground(packageName: String?) {
        if (packageName == null) {
            return
        }

        val targets = TargetPackages.getSelected(this)
        if (targets.isEmpty()) {
            disableNfcIfNeeded()
            isTargetAppForeground = false
            return
        }

        if (packageName in targets) {
            handleTargetEntered(packageName)
        } else {
            handleTargetLeft()
        }
    }

    private fun handleTargetEntered(packageName: String) {
        val reenteredAfterLeave = !isTargetAppForeground
        val nfcWasOff = !NfcController.isNfcEnabled(this)

        NfcController.setNfcEnabled(this, true)

        // Reload when switching back to a paused Wallet task, not only on cold start.
        // Wallet keeps its old screen in recents and won't re-check NFC unless we restart it.
        if (reenteredAfterLeave || nfcWasOff) {
            scheduleRestartWhenNfcOn(
                packageName = packageName,
                bypassCooldown = reenteredAfterLeave,
            )
        }
        isTargetAppForeground = true
    }

    private fun handleTargetLeft() {
        cancelPendingRestart()
        disableNfcIfNeeded()
        isTargetAppForeground = false
    }

    private fun disableNfcIfNeeded() {
        if (!NfcController.isNfcEnabled(this)) {
            return
        }
        if (NfcController.setNfcEnabled(this, false)) {
            return
        }
        handler.postDelayed({ retryDisableNfc() }, NFC_POLL_MS)
    }

    private fun retryDisableNfc() {
        if (isTargetAppForeground) {
            return
        }
        if (NfcController.isNfcEnabled(this)) {
            NfcController.setNfcEnabled(this, false)
        }
    }

    private fun scheduleRestartWhenNfcOn(
        packageName: String,
        attemptsLeft: Int = 20,
        bypassCooldown: Boolean = false,
    ) {
        pendingRestartPackage = packageName
        pendingRestartBypassCooldown = bypassCooldown
        handler.postDelayed({
            if (pendingRestartPackage != packageName) {
                return@postDelayed
            }
            if (NfcController.isNfcEnabled(this)) {
                maybeRestartPendingTarget()
            } else if (attemptsLeft > 0) {
                scheduleRestartWhenNfcOn(
                    packageName = packageName,
                    attemptsLeft = attemptsLeft - 1,
                    bypassCooldown = bypassCooldown,
                )
            } else {
                pendingRestartPackage = null
                pendingRestartBypassCooldown = false
            }
        }, NFC_POLL_MS)
    }

    private fun maybeRestartPendingTarget() {
        val packageName = pendingRestartPackage ?: return
        if (lastForegroundPackage != packageName) {
            pendingRestartPackage = null
            pendingRestartBypassCooldown = false
            return
        }
        if (!NfcController.isNfcEnabled(this)) {
            return
        }

        val now = System.currentTimeMillis()
        val bypassCooldown = pendingRestartBypassCooldown
        if (!bypassCooldown && now - lastRestartAtMs < RESTART_COOLDOWN_MS) {
            return
        }

        if (AppLauncher.restartApp(this, packageName)) {
            lastRestartAtMs = now
            Log.i(TAG, "Reloaded $packageName after enabling NFC")
        }
        pendingRestartPackage = null
        pendingRestartBypassCooldown = false
    }

    private fun cancelPendingRestart() {
        pendingRestartPackage = null
        pendingRestartBypassCooldown = false
    }

    private fun isTransientOverlay(packageName: String): Boolean =
        packageName in TRANSIENT_OVERLAY_PACKAGES

    companion object {
        private const val TAG = "TimeToPay"
        private const val NFC_POLL_MS = 100L
        private const val RESTART_COOLDOWN_MS = 3000L

        // Quick panel, notification shade, and other system UI overlays — not real app switches.
        private val TRANSIENT_OVERLAY_PACKAGES = setOf(
            "com.google.android.apps.wearable.systemui",
            "com.google.android.wearable.systemui",
            "com.android.systemui",
            "com.samsung.android.wearable.systemui",
        )
    }
}
