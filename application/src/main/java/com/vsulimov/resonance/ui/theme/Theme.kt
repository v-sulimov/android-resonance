package com.vsulimov.resonance.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import com.vsulimov.resonance.core.DeviceCompat

// region Fallback Color Schemes

private val LightColorScheme: ColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    surfaceVariant = md_theme_light_surfaceVariant,
    surfaceContainerLow = md_theme_light_surfaceContainerLow,
    surfaceContainerHigh = md_theme_light_surfaceContainerHigh,
    outline = md_theme_light_outline,
    outlineVariant = md_theme_light_outlineVariant,
    error = md_theme_light_error,
    onError = md_theme_light_onError,
    errorContainer = md_theme_light_errorContainer,
    onErrorContainer = md_theme_light_onErrorContainer,
    inverseSurface = md_theme_light_inverseSurface,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    scrim = md_theme_light_scrim
)

private val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    surfaceVariant = md_theme_dark_surfaceVariant,
    surfaceContainerLow = md_theme_dark_surfaceContainerLow,
    surfaceContainerHigh = md_theme_dark_surfaceContainerHigh,
    outline = md_theme_dark_outline,
    outlineVariant = md_theme_dark_outlineVariant,
    error = md_theme_dark_error,
    onError = md_theme_dark_onError,
    errorContainer = md_theme_dark_errorContainer,
    onErrorContainer = md_theme_dark_onErrorContainer,
    inverseSurface = md_theme_dark_inverseSurface,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    scrim = md_theme_dark_scrim
)

// endregion

/**
 * Resonance application theme.
 *
 * Applies Material 3 color, typography, and shape tokens. On devices that support
 * Material You, dynamic color is used; all others fall back to the violet palette
 * generated from the logo seed color.
 *
 * Custom non-M3 colors (success banner) are provided via [LocalResonanceColors]
 * and accessed through [ResonanceTheme.extendedColors].
 *
 * @param darkTheme Whether to apply the dark color scheme. Defaults to system setting.
 * @param dynamicColor Whether to use Material You dynamic color on supported devices.
 * @param content The composable content to theme.
 */
@Composable
fun ResonanceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && DeviceCompat.isDynamicColorAvailable() -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val extendedColors = if (darkTheme) DarkResonanceColors else LightResonanceColors

    CompositionLocalProvider(LocalResonanceColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}

/**
 * Convenience accessor for Resonance theme values.
 *
 * Usage:
 * ```
 * val bg = ResonanceTheme.extendedColors.successContainer
 * ```
 */
object ResonanceTheme {

    /**
     * Custom color tokens not part of the standard M3 [ColorScheme].
     */
    val extendedColors: ResonanceColors
        @Composable
        @ReadOnlyComposable
        get() = LocalResonanceColors.current
}
