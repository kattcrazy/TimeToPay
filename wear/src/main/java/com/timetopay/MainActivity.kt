package com.timetopay

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.timetopay.theme.TimeToPayTheme

class MainActivity : ComponentActivity() {
    private val refreshState = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TimeToPayTheme {
                val refreshTick = refreshState.intValue
                MainScreen(
                    refreshTick = refreshTick,
                    onChooseApps = {
                        startActivity(Intent(this, AppPickerActivity::class.java))
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshState.intValue++
    }
}

@Composable
private fun MainScreen(refreshTick: Int, onChooseApps: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    @Suppress("UNUSED_VARIABLE")
    val unusedRefresh = refreshTick

    AppScaffold {
        ScreenScaffold {
            TransformingLazyColumn(
                modifier = Modifier.padding(horizontal = 8.dp),
                state = rememberTransformingLazyColumnState(),
            ) {
                item {
                    StatusLine(
                        label = stringResource(R.string.status_secure_settings),
                        value = yesNo(context, SetupStatus.hasSecureSettingsPermission(context)),
                    )
                }
                item {
                    StatusLine(
                        label = stringResource(R.string.status_accessibility),
                        value = yesNo(context, SetupStatus.isAccessibilityServiceEnabled(context)),
                    )
                }
                item {
                    StatusLine(
                        label = stringResource(R.string.status_apps_selected),
                        value = TargetPackages.getSelected(context).size.toString(),
                    )
                }
                item {
                    StatusLine(
                        label = stringResource(R.string.status_nfc),
                        value = if (NfcController.isNfcEnabled(context)) {
                            stringResource(R.string.status_on)
                        } else {
                            stringResource(R.string.status_off)
                        },
                    )
                }
                item {
                    Button(
                        onClick = onChooseApps,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    ) {
                        Text(stringResource(R.string.choose_apps_button))
                    }
                }
                item {
                    Text(
                        text = hintText(context),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    )
                }
                item { CreditFooter() }
            }
        }
    }
}

@Composable
private fun StatusLine(label: String, value: String) {
    Text(
        text = "$label: $value",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    )
}

@Composable
private fun hintText(context: android.content.Context): String {
    val selectedCount = TargetPackages.getSelected(context).size
    return when {
        !SetupStatus.hasSecureSettingsPermission(context) ||
            !SetupStatus.isAccessibilityServiceEnabled(context) ->
            context.getString(R.string.setup_hint)
        selectedCount == 0 ->
            context.getString(R.string.no_apps_selected_hint)
        else ->
            context.getString(R.string.ready_hint)
    }
}

@Composable
fun CreditFooter(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.credit_by),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 2.dp),
    )
    Text(
        text = stringResource(R.string.credit_repo),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
    )
}

private fun yesNo(context: android.content.Context, value: Boolean): String =
    context.getString(if (value) R.string.status_yes else R.string.status_no)
