package kattcrazy.timetopay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun ObserveLiveSetupStatus(
    refreshTick: Int,
    onRefresh: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, onRefresh) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onRefresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(context, onRefresh) {
        val nfcReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == NfcAdapter.ACTION_ADAPTER_STATE_CHANGED) {
                    onRefresh()
                }
            }
        }
        val nfcFilter = IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(nfcReceiver, nfcFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(nfcReceiver, nfcFilter)
        }

        val selectionListener = TargetPackages.registerOnSelectionChanged(context) {
            onRefresh()
        }

        val settingsHandler = Handler(Looper.getMainLooper())
        val settingsObserver = object : ContentObserver(settingsHandler) {
            override fun onChange(selfChange: Boolean) {
                onRefresh()
            }
        }
        context.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),
            false,
            settingsObserver,
        )
        context.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_ENABLED),
            false,
            settingsObserver,
        )

        onDispose {
            context.unregisterReceiver(nfcReceiver)
            TargetPackages.unregisterOnSelectionChanged(context, selectionListener)
            context.contentResolver.unregisterContentObserver(settingsObserver)
        }
    }

    LaunchedEffect(refreshTick) {
        if (NfcController.nfcStatus(context) != NfcStatus.TurningOn) {
            return@LaunchedEffect
        }
        while (isActive && NfcController.nfcStatus(context) == NfcStatus.TurningOn) {
            delay(150)
            onRefresh()
        }
    }
}
