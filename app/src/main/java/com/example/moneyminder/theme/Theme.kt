package com.example.moneyminder.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = VioletPrimary,
    onPrimary = Color(0xFF1A0B2E),
    primaryContainer = VioletDeep,
    onPrimaryContainer = TextPrimary,
    inversePrimary = VioletSoft,
    secondary = PinkAccent,
    onSecondary = Color(0xFF2A0B1F),
    secondaryContainer = CardBackgroundElevated,
    onSecondaryContainer = TextPrimary,
    tertiary = VioletSoft,
    onTertiary = Color(0xFF1A0B2E),
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = CardBackground,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = BackgroundDark,
    surfaceContainerLow = SurfaceDark,
    surfaceContainer = CardBackground,
    surfaceContainerHigh = CardBackgroundElevated,
    surfaceContainerHighest = GlassSelectionHighlight,
    outline = CardBorder,
    outlineVariant = CardBorderSubtle,
    error = ExpenseRed,
    onError = Color(0xFF2A0B12)
)

@Composable
fun MoneyMinderTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = BackgroundDark.toArgb()
            window.navigationBarColor = BackgroundDark.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
