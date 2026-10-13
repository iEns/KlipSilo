package com.phaprodevelop.klipsilo.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme =
    lightColorScheme(
        primary = SnipPrimary,
        onPrimary = SnipOnPrimary,
        primaryContainer = Purple80,
        secondary = SnipSecondary,
        onSecondary = SnipOnSecondary,
        background = SnipBackground,
        onBackground = SnipOnBackground,
        surface = SnipSurface,
        onSurface = SnipOnSurface,
        error = SnipError,
        onError = SnipOnError,
    )

private val DarkColorScheme =
    darkColorScheme(
        primary = SnipDarkPrimary,
        onPrimary = SnipDarkOnPrimary,
        primaryContainer = PurpleGrey80,
        secondary = SnipDarkSecondary,
        onSecondary = SnipDarkOnSecondary,
        background = SnipDarkBackground,
        onBackground = SnipDarkOnBackground,
        surface = SnipDarkSurface,
        onSurface = SnipDarkOnSurface,
        error = SnipDarkError,
        onError = SnipDarkOnError,
    )

@Composable
fun KlipSiloTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
