package kattcrazy.timetopay

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
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
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import kattcrazy.timetopay.theme.TimeToPayTheme

class MainActivity : ComponentActivity() {
    private val refreshState = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            TimeToPayTheme {
                val refreshTick = refreshState.intValue
                MainScreen(
                    refreshTick = refreshTick,
                    onRefresh = { refreshState.intValue++ },
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
private fun MainScreen(refreshTick: Int, onRefresh: () -> Unit, onChooseApps: () -> Unit) {
    val context = LocalContext.current

    ObserveLiveSetupStatus(refreshTick = refreshTick, onRefresh = onRefresh)

    val hasAccessibility = remember(refreshTick) {
        SetupStatus.isAccessibilityServiceEnabled(context)
    }
    val hasSecureSettings = remember(refreshTick) {
        SetupStatus.hasSecureSettingsPermission(context)
    }
    val nfcStatus = remember(refreshTick) {
        NfcController.nfcStatus(context)
    }
    val selectedCount = remember(refreshTick) {
        TargetPackages.getSelected(context).size
    }

    AppScaffold {
        val listState = rememberTransformingLazyColumnState()
        val transformationSpec = rememberTransformationSpec()
        ScreenScaffold(scrollState = listState) { contentPadding ->
            TransformingLazyColumn(
                contentPadding = contentPadding,
                state = listState,
            ) {
                topScrollSpacer(transformationSpec = transformationSpec)

                item {
                    StatusLine(
                        label = stringResource(R.string.status_secure_settings),
                        value = yesNo(context, hasSecureSettings),
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                    )
                }
                item {
                    StatusLine(
                        label = stringResource(R.string.status_accessibility),
                        value = yesNo(context, hasAccessibility),
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                    )
                }
                item {
                    StatusLine(
                        label = stringResource(R.string.status_apps_selected),
                        value = selectedCount.toString(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                    )
                }
                item {
                    StatusLine(
                        label = stringResource(R.string.status_nfc),
                        value = when (nfcStatus) {
                            NfcStatus.On -> stringResource(R.string.status_on)
                            NfcStatus.TurningOn -> stringResource(R.string.status_turning_on)
                            NfcStatus.Off -> stringResource(R.string.status_off)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                    )
                }

                if (!hasAccessibility) {
                    item {
                        Button(
                            onClick = { SetupIntents.openAccessibilitySettings(context) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .transformedHeight(this, transformationSpec),
                            transformation = SurfaceTransformation(transformationSpec),
                        ) {
                            Text(stringResource(R.string.open_accessibility_button))
                        }
                    }
                    item {
                        Button(
                            onClick = { SetupIntents.openAppInfo(context) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .transformedHeight(this, transformationSpec),
                            transformation = SurfaceTransformation(transformationSpec),
                        ) {
                            Text(stringResource(R.string.open_app_info_button))
                        }
                    }
                }

                item {
                    Button(
                        onClick = onChooseApps,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec),
                    ) {
                        Text(stringResource(R.string.choose_apps_button))
                    }
                }
                item {
                    Text(
                        text = hintText(context, hasAccessibility, hasSecureSettings),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .transformedHeight(this, transformationSpec),
                    )
                }
                item {
                    CreditFooter(
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                    )
                }

                bottomScrollSpacer(transformationSpec = transformationSpec)
            }
        }
    }
}

@Composable
private fun StatusLine(label: String, value: String, modifier: Modifier = Modifier) {
    Text(
        text = "$label: $value",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        modifier = modifier.padding(vertical = 2.dp),
    )
}

@Composable
private fun hintText(
    context: android.content.Context,
    hasAccessibility: Boolean,
    hasSecureSettings: Boolean,
): String {
    val selectedCount = TargetPackages.getSelected(context).size
    return when {
        !hasAccessibility ->
            context.getString(R.string.setup_hint_accessibility)
        !hasSecureSettings ->
            context.getString(R.string.setup_hint_secure_settings)
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
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
    )
}

private fun yesNo(context: android.content.Context, value: Boolean): String =
    context.getString(if (value) R.string.status_yes else R.string.status_no)
