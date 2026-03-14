package com.voicedeutsch.master.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary            = Primary,
    primaryContainer   = PrimaryContainer,
    secondary          = Secondary,
    secondaryContainer = SecondaryContainer,
    tertiary           = Tertiary,
    background         = Background,
    surface            = Surface,
    surfaceVariant     = SurfaceVariant,
    onBackground       = OnBackground,
    onSurface          = OnBackground,
    onSurfaceVariant   = OnSurfaceVariant,
    outline            = Outline,
    error              = Error,
)

private val DarkColorScheme = darkColorScheme(
    primary            = Color(0xFF7AB8FF),
    primaryContainer   = Color(0xFF1A3A5C),
    secondary          = Color(0xFFBB86FC),
    secondaryContainer = Color(0xFF3A1F5C),
    tertiary           = Color(0xFF6EE7B7),
    background         = Color(0xFF0D1117),
    surface            = Color(0xFF161B22),
    surfaceVariant     = Color(0xFF21262D),
    onBackground       = Color(0xFFC9D1D9),
    onSurface          = Color(0xFFC9D1D9),
    onSurfaceVariant   = Color(0xFF8B949E),
    outline            = Color(0xFF30363D),
    error              = Color(0xFFFF7B72),
)

@Composable
fun VoiceDeutschMasterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = VoiceDeutschTypography,
        shapes      = VoiceDeutschShapes,
        content     = content,
    )
}