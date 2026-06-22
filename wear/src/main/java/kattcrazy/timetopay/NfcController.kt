package kattcrazy.timetopay

import android.annotation.SuppressLint
import android.content.Context
import android.nfc.NfcAdapter
import android.provider.Settings
import android.util.Log

enum class NfcStatus {
    On,
    TurningOn,
    Off,
}

object NfcController {
    private const val TAG = "TimeToPay"

    fun hasSecureSettings(context: Context): Boolean =
        SetupStatus.hasSecureSettingsPermission(context)

    fun nfcStatus(context: Context): NfcStatus {
        val adapter = NfcAdapter.getDefaultAdapter(context) ?: return NfcStatus.Off
        return when (getAdapterState(adapter)) {
            NfcAdapter.STATE_ON -> NfcStatus.On
            NfcAdapter.STATE_TURNING_ON -> NfcStatus.TurningOn
            else -> NfcStatus.Off
        }
    }

    fun isNfcEnabled(context: Context): Boolean =
        nfcStatus(context) == NfcStatus.On

    @SuppressLint("MissingPermission")
    fun setNfcEnabled(context: Context, enabled: Boolean): Boolean {
        if (!hasSecureSettings(context)) {
            Log.w(TAG, "Missing WRITE_SECURE_SETTINGS")
            return false
        }

        val adapter = NfcAdapter.getDefaultAdapter(context)
        if (adapter == null) {
            Log.w(TAG, "No NFC adapter")
            return false
        }

        val currentStatus = nfcStatus(context)
        if (enabled && currentStatus == NfcStatus.On) {
            return true
        }
        if (!enabled && currentStatus == NfcStatus.Off) {
            return true
        }

        val adapterAccepted = setNfcViaAdapter(adapter, enabled)
        if (adapterAccepted) {
            Log.i(TAG, "NfcAdapter ${if (enabled) "enable" else "disable"} accepted")
        } else {
            Log.w(TAG, "NfcAdapter ${if (enabled) "enable" else "disable"} rejected, trying settings")
            setNfcViaSettings(context, enabled)
        }

        val status = nfcStatus(context)
        val reached = if (enabled) {
            status == NfcStatus.On || status == NfcStatus.TurningOn
        } else {
            status == NfcStatus.Off
        }
        Log.i(
            TAG,
            "NFC ${if (enabled) "on" else "off"} requested, reached=$reached, status=$status, accepted=$adapterAccepted",
        )
        return reached || adapterAccepted
    }

    private fun getAdapterState(adapter: NfcAdapter): Int {
        return try {
            NfcAdapter::class.java.getMethod("getAdapterState").invoke(adapter) as Int
        } catch (exception: ReflectiveOperationException) {
            if (adapter.isEnabled) NfcAdapter.STATE_ON else NfcAdapter.STATE_OFF
        }
    }

    private fun setNfcViaAdapter(adapter: NfcAdapter, enabled: Boolean): Boolean {
        return try {
            if (enabled) {
                invokeAdapterMethod(adapter, "enable") ?: false
            } else {
                invokeAdapterMethod(adapter, "disable", true)
                    ?: invokeAdapterMethod(adapter, "disable")
                    ?: false
            }
        } catch (exception: ReflectiveOperationException) {
            Log.w(TAG, "NfcAdapter reflection failed", exception)
            false
        }
    }

    private fun invokeAdapterMethod(adapter: NfcAdapter, name: String, booleanArg: Boolean? = null): Boolean? {
        return try {
            val result = if (booleanArg == null) {
                NfcAdapter::class.java.getMethod(name).invoke(adapter)
            } else {
                NfcAdapter::class.java
                    .getMethod(name, Boolean::class.javaPrimitiveType)
                    .invoke(adapter, booleanArg)
            }
            result as? Boolean
        } catch (exception: ReflectiveOperationException) {
            null
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
