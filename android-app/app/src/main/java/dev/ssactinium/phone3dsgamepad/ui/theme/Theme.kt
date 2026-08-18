package dev.ssactinium.phone3dsgamepad.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val Scheme = darkColorScheme(
    primary = HingeGold,
    onPrimary = Housing,
    background = Housing,
    onBackground = Ink,
    surface = HousingRaised,
    onSurface = Ink,
    secondary = LedLive,
    error = LedDead,
)

@Composable
fun HingePadTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Scheme,
        content = content,
    )
}
