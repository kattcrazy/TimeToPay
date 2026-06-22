package kattcrazy.timetopay.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.MaterialTheme

@Composable
fun TimeToPayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TimeToPayColorScheme.wearDark,
        content = content,
    )
}
