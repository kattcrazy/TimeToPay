package com.timetopay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.timetopay.theme.TimeToPayTheme

class AppPickerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TimeToPayTheme {
                AppPickerScreen(
                    onSave = { selected ->
                        TargetPackages.setSelected(this, selected)
                        finish()
                    },
                )
            }
        }
    }
}

@Composable
private fun AppPickerScreen(onSave: (Set<String>) -> Unit) {
    val context = LocalContext.current
    val apps = remember { LaunchableApps.query(context) }
    var selected by remember { mutableStateOf(TargetPackages.getSelected(context)) }

    AppScaffold {
        ScreenScaffold {
            TransformingLazyColumn(
                modifier = Modifier.padding(horizontal = 8.dp),
                state = rememberTransformingLazyColumnState(),
            ) {
                item {
                    ListHeader {
                        Text(stringResource(R.string.choose_apps))
                    }
                }
                items(apps, key = { it.packageName }) { app ->
                    val isSelected = app.packageName in selected
                    Button(
                        onClick = {
                            selected = if (isSelected) {
                                selected - app.packageName
                            } else {
                                selected + app.packageName
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        colors = if (isSelected) {
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        } else {
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        },
                    ) {
                        Text(app.label)
                    }
                }
                item {
                    Button(
                        onClick = { onSave(selected) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
                item { CreditFooter() }
            }
        }
    }
}
