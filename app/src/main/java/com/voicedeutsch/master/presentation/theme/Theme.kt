package com.voicedeutsch.master.presentation.theme

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

// ── Dark colour scheme (primary) ──────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary            = Primary,
    onPrimary          = OnBackground,
    primaryContainer   = PrimaryDark,
    onPrimaryContainer = PrimaryLight,

    secondary            = Secondary,
    onSecondary          = OnBackground,
    secondaryContainer   = SecondaryDark,
    onSecondaryContainer = SecondaryLight,

    tertiary            = Tertiary,
    onTertiary          = OnBackground,
    tertiaryContainer   = Tertiary.copy(alpha = 0.3f),
    onTertiaryContainer = TertiaryLight,

    error            = Error,
    onError          = OnBackground,
    errorContainer   = ErrorDark,
    onErrorContainer = Error,

    background         = Background,
    onBackground       = OnBackground,

    surface            = Surface,
    onSurface          = OnBackground,
    surfaceVariant     = SurfaceVariant,
    onSurfaceVariant   = OnBackgroundMuted,

    outline            = Outline,
    outlineVariant     = OutlineVariant,

    inverseSurface     = OnBackground,
    inverseOnSurface   = Background,
    inversePrimary     = PrimaryDark,
)

// ── Light colour scheme (fallback) ────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary            = Primary,
    onPrimary          = OnBackground,
    primaryContainer   = PrimaryLight.copy(alpha = 0.15f),
    onPrimaryContainer = PrimaryDark,

    secondary            = Secondary,
    onSecondary          = OnBackground,
    secondaryContainer   = SecondaryLight.copy(alpha = 0.15f),
    onSecondaryContainer = SecondaryDark,

    tertiary            = Tertiary,
    onTertiary          = OnBackground,

    error            = Error,
    onError          = OnBackground,

    background         = BackgroundLight,
    onBackground       = OnBackgroundLight,

    surface            = SurfaceLight,
    onSurface          = OnBackgroundLight,
    surfaceVariant     = Color(0xFFEDE8F5).let { it }, // use raw value
    onSurfaceVariant   = OnBackgroundMutedLight,
)

/**
 * Root theme composable for the entire VoiceDeutschMaster app.
 *
 * @param darkTheme     Override dark/light — defaults to system setting.
 * @param dynamicColor  Android 12+ Material You dynamic colours. Disabled by
 *                      default to preserve the Voice brand palette.
 */
@Composable
fun VoiceDeutschMasterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    // Make the status bar transparent and match the theme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = VoiceDeutschTypography,
        shapes      = VoiceDeutschShapes,
        content     = content,
    )
}

// Convenience extension — needed in Color.kt reference above
@Suppress("NOTHING_TO_INLINE")
private inline fun androidx.compose.ui.graphics.Color.let(block: (androidx.compose.ui.graphics.Color) -> androidx.compose.ui.graphics.Color) = block(this)
