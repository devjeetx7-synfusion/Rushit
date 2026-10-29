package com.rushit.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val RushColors = lightColorScheme(
    primary = Coral,
    onPrimary = CardWhite,
    secondary = Amber,
    onSecondary = Ink,
    background = Cream,
    onBackground = Ink,
    surface = CardWhite,
    onSurface = Ink,
    surfaceVariant = Peach,
    onSurfaceVariant = Muted,
    outline = Border,
    error = DeepRed,
    onError = CardWhite,
)

@Composable
fun RushItTheme(content: @Composable () -> Unit) {
    // The reference intentionally stays warm and light even when the system is in dark mode.
    MaterialTheme(colorScheme = RushColors, typography = RushTypography, content = content)
}
