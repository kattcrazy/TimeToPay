package com.timetopay

import android.annotation.SuppressLint
import android.content.Context
import android.nfc.NfcAdapter
import android.provider.Settings
import android.util.Log

object NfcController {
    private const val TAG = "TimeToPay"

    fun hasSecureSettings(context: Context): Boolean =
        SetupStatus.hasSecureSettingsPermission(context)

    fun isNfcEnabled(context: Context): Boolean =
        NfcAdapter.getDefaultAdapter(context)?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun setNfcEnabled(context: Context, enabled: Boolean): Boolean {
        if (!hasSecureSettings(context)) {
            return false
        }

        val adapter = NfcAdapter.getDefaultAdapter(context) ?: return false

        return if (setNfcViaAdapter(adapter, enabled)) {
            true
        } else {
            setNfcViaSettings(context, enabled)
        }
    }

    private fun setNfcViaAdapter(adapter: NfcAdapter, enabled: Boolean): Boolean {
        return try {
            if (enabled) {
                NfcAdapter::class.java.getMethod("enable").invoke(adapter) as Boolean
            } else {
                NfcAdapter::class.java
                    .getMethod("disable", Boolean::class.javaPrimitiveType)
                    .invoke(adapter, true) as Boolean
            }
        } catch (exception: ReflectiveOperationException) {
            Log.w(TAG, "NfcAdapter reflection failed", exception)
            false
        }
    }

    private fun setNfcViaSettings(context: Context, enabled: Boolean): Boolean {
        return try {
            Settings.Global.putInt(
                context.contentResolver,
                "nfc_on",
                if (enabled) 1 else 0,
            )
            true
        } catch (exception: Exception) {
            Log.w(TAG, "Settings.Global nfc_on failed", exception)
            false
        }
    }
}
