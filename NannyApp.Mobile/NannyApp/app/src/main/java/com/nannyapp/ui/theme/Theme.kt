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
    secondary = Color(0xFF176697),
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
    primary = Color(0xFF8EC9F8),
    onPrimary = Color(0xFF082E50),
    primaryContainer = Color(0xFF123F70),
    onPrimaryContainer = Color(0xFFD9EFFF),
    secondary = Color(0xFF93CFF5),
    onSecondary = Color(0xFF07304B),
    secondaryContainer = Color(0xFF204760),
    onSecondaryContainer = Color(0xFFD9EFFF),
    tertiary = Color(0xFF79DDB4),
    onTertiary = Color(0xFF003824),
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF28394D),
    onSurfaceVariant = Color(0xFFB8C8D9),
    outline = Color(0xFF8C9FB4),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
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
            val activity = view.context as? Activity ?: return@SideEffect
            val window = activity.window
            window.statusBarColor = colorScheme.surface.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = NannyTypography,
        shapes = NannyShapes,
        content = content,
    )
}
