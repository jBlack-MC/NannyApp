package com.nannyapp.ui.theme

import androidx.compose.ui.graphics.Color
import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = NannyPrimary,
    onPrimary = NannySurface,
    primaryContainer = NannyPrimaryContainer,
    onPrimaryContainer = NannyPrimaryDark,
    secondary = NannySecondary,
    onSecondary = NannySurface,
    secondaryContainer = NannySecondaryContainer,
    tertiary = NannyTertiary,
    background = NannyBackground,
    onBackground = NannyOnSurface,
    surface = NannySurface,
    onSurface = NannyOnSurface,
    surfaceVariant = NannySurfaceVariant,
    onSurfaceVariant = NannyOnSurfaceVariant,
    outline = NannyOutline,
    error = NannyError,
)

private val DarkColors = darkColorScheme(
    primary = NannyPrimary,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = NannyPrimaryDark,
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = NannySecondary,
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC3C7CF),
    error = NannyError,
)

@Composable
fun NannyAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // keep the NannyApp brand identity rather than Material You
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(LocalContext.current) else dynamicLightColorScheme(LocalContext.current)
        darkTheme -> DarkColors
        else -> LightColors
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = NannyTypography,
        shapes = NannyShapes,
        content = content,
    )
}
