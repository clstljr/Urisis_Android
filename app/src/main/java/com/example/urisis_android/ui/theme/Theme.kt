package com.example.urisis_android.ui.theme

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Material 3 colour palettes for Urisis.
 *
 * The light scheme keeps the existing brand blue. The dark scheme is
 * tuned for medical-app readability at night: deep navy backgrounds (not
 * pure black), softer blue accents, and tinted surface variants so cards
 * remain distinguishable from the background.
 *
 * Dynamic colour (Material You, Android 12+) is opt-in via [dynamicColor]
 * and disabled by default — we want the brand blue to persist regardless
 * of the user's wallpaper.
 */
private val LightColors = lightColorScheme(
    primary             = BrandPrimary,
    onPrimary           = Color.White,
    primaryContainer    = Color(0xFFD6E7FF),
    onPrimaryContainer  = Color(0xFF001A40),

    secondary           = BrandSecondary,
    onSecondary         = Color.White,
    secondaryContainer  = Color(0xFFB2EBF2),
    onSecondaryContainer = Color(0xFF002930),

    tertiary            = BrandPrimaryAccent,
    onTertiary          = Color.White,

    background          = Color(0xFFF7F9FC),
    onBackground        = Color(0xFF1A1C1E),
    surface             = Color.White,
    onSurface           = Color(0xFF1A1C1E),
    surfaceVariant      = Color(0xFFEEF1F5),
    onSurfaceVariant    = Color(0xFF42474E),
    outline             = Color(0xFFE5E7EB),
    outlineVariant      = Color(0xFFD0D4DA),

    error               = HydrationRed,
    onError             = Color.White,
    errorContainer      = Color(0xFFFEE2E2),
    onErrorContainer    = Color(0xFF7F1D1D),
)

private val DarkColors = darkColorScheme(
    primary             = BrandPrimaryDark,
    onPrimary           = Color(0xFF002F66),
    primaryContainer    = Color(0xFF004586),
    onPrimaryContainer  = Color(0xFFD6E7FF),

    secondary           = BrandSecondaryDark,
    onSecondary         = Color(0xFF00363D),
    secondaryContainer  = Color(0xFF004F58),
    onSecondaryContainer = Color(0xFF6FF7FE),

    tertiary            = BrandPrimaryAccentDark,
    onTertiary          = Color(0xFF002F66),

    // Deep navy — easier on the eyes than pure black for medical UIs at night
    background          = Color(0xFF0B1220),
    onBackground        = Color(0xFFE6EAF2),
    surface             = Color(0xFF111827),
    onSurface           = Color(0xFFE6EAF2),
    surfaceVariant      = Color(0xFF1F2937),
    onSurfaceVariant    = Color(0xFFB9C2CD),
    outline             = Color(0xFF374151),
    outlineVariant      = Color(0xFF1F2937),

    error               = Color(0xFFFCA5A5),
    onError             = Color(0xFF601410),
    errorContainer      = Color(0xFF7F1D1D),
    onErrorContainer    = Color(0xFFFEE2E2),
)

@Composable
fun UrisisTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            // Match the system bars (status / nav) to the theme so headers
            // blend into the device chrome instead of clashing with it.
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat
                .getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,      // defined in Type.kt
        content     = content,
    )
}

/**
 * Brand gradient used by the splash, login header, register header, and
 * dashboard "Start Test" card. Adapts to dark mode so it's vivid on
 * white backgrounds and softened on dark backgrounds.
 */
@Composable
fun brandBrush(darkTheme: Boolean = isSystemInDarkTheme()): Brush {
    val a = if (darkTheme) Color(0xFF1E3A8A) else BrandPrimary
    val b = if (darkTheme) Color(0xFF0E7490) else BrandPrimaryAccent
    return Brush.linearGradient(listOf(a, b))
}

private fun Color.toArgb(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(),
    (red   * 255).toInt(),
    (green * 255).toInt(),
    (blue  * 255).toInt(),
)