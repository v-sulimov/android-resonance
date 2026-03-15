package com.vsulimov.resonance.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// region Light M3 Palette — Generated from seed #A06CEF (violet, matching logo)

val md_theme_light_primary = Color(0xFF7540C2)
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = Color(0xFFECDCFF)
val md_theme_light_onPrimaryContainer = Color(0xFF280056)
val md_theme_light_secondary = Color(0xFF645A70)
val md_theme_light_secondaryContainer = Color(0xFFEBDEF7)
val md_theme_light_onSecondaryContainer = Color(0xFF1F182A)
val md_theme_light_tertiary = Color(0xFF7F525B)
val md_theme_light_tertiaryContainer = Color(0xFFFFD9DF)
val md_theme_light_onTertiaryContainer = Color(0xFF32101A)
val md_theme_light_surface = Color(0xFFFFFBFF)
val md_theme_light_onSurface = Color(0xFF1D1B1E)
val md_theme_light_onSurfaceVariant = Color(0xFF4A454E)
val md_theme_light_surfaceVariant = Color(0xFFE8E0EB)
val md_theme_light_surfaceContainerLow = Color(0xFFF8F2F7)
val md_theme_light_surfaceContainerHigh = Color(0xFFECE6EB)
val md_theme_light_outline = Color(0xFF7B757F)
val md_theme_light_outlineVariant = Color(0xFFCBC4CF)
val md_theme_light_error = Color(0xFFBA1A1A)
val md_theme_light_onError = Color(0xFFFFFFFF)
val md_theme_light_errorContainer = Color(0xFFFFDAD6)
val md_theme_light_onErrorContainer = Color(0xFF410002)
val md_theme_light_inverseSurface = Color(0xFF322F33)
val md_theme_light_inverseOnSurface = Color(0xFFF5EFF4)
val md_theme_light_scrim = Color(0xFF000000)

// endregion

// region Dark M3 Palette — Generated from seed #A06CEF (violet, matching logo)

val md_theme_dark_primary = Color(0xFFD6BAFF)
val md_theme_dark_onPrimary = Color(0xFF430089)
val md_theme_dark_primaryContainer = Color(0xFF5C22A8)
val md_theme_dark_onPrimaryContainer = Color(0xFFECDCFF)
val md_theme_dark_secondary = Color(0xFFCEC2DB)
val md_theme_dark_secondaryContainer = Color(0xFF4C4357)
val md_theme_dark_onSecondaryContainer = Color(0xFFEBDEF7)
val md_theme_dark_tertiary = Color(0xFFF1B7C3)
val md_theme_dark_tertiaryContainer = Color(0xFF643B44)
val md_theme_dark_onTertiaryContainer = Color(0xFFFFD9DF)
val md_theme_dark_surface = Color(0xFF1D1B1E)
val md_theme_dark_onSurface = Color(0xFFE7E1E6)
val md_theme_dark_onSurfaceVariant = Color(0xFFCBC4CF)
val md_theme_dark_surfaceVariant = Color(0xFF4A454E)
val md_theme_dark_surfaceContainerLow = Color(0xFF211F22)
val md_theme_dark_surfaceContainerHigh = Color(0xFF363438)
val md_theme_dark_outline = Color(0xFF958E99)
val md_theme_dark_outlineVariant = Color(0xFF4A454E)
val md_theme_dark_error = Color(0xFFFFB4AB)
val md_theme_dark_onError = Color(0xFF690005)
val md_theme_dark_errorContainer = Color(0xFF93000A)
val md_theme_dark_onErrorContainer = Color(0xFFFFB4AB)
val md_theme_dark_inverseSurface = Color(0xFFE7E1E6)
val md_theme_dark_inverseOnSurface = Color(0xFF322F33)
val md_theme_dark_scrim = Color(0xFF000000)

// endregion

// region Custom Colors

/**
 * Extended color tokens not covered by M3 [ColorScheme].
 *
 * Provided via [LocalResonanceColors] composition local and accessed
 * through [ResonanceTheme.extendedColors].
 */
@Immutable
data class ResonanceColors(
    /** Background color for the success banner surface. */
    val successContainer: Color,
    /** Text color used on the success container surface. */
    val onSuccessContainer: Color,
    /** Tint color for the success banner leading icon. */
    val successIcon: Color
)

/** Extended color tokens for the light theme. */
val LightResonanceColors = ResonanceColors(
    successContainer = Color(0xFFD4F5D0),
    onSuccessContainer = Color(0xFF1B3A18),
    successIcon = Color(0xFF2E7D32)
)

/** Extended color tokens for the dark theme. */
val DarkResonanceColors = ResonanceColors(
    successContainer = Color(0xFF1B3A18),
    onSuccessContainer = Color(0xFFD4F5D0),
    successIcon = Color(0xFF81C784)
)

/** CompositionLocal providing [ResonanceColors] for the current theme. */
val LocalResonanceColors = staticCompositionLocalOf { LightResonanceColors }

// endregion

// region Logo Gradient Colors

/** Welcome-screen logo gradient stops for light theme (135 deg, top-left to bottom-right). */
val LogoGradientLight = listOf(
    Color(0xFF4A5BC7),
    Color(0xFF6B5CE7),
    Color(0xFF8B6CEF)
)

/** Welcome-screen logo gradient stops for dark theme (135 deg, top-left to bottom-right). */
val LogoGradientDark = listOf(
    Color(0xFF6750A4),
    Color(0xFF7B66C7),
    Color(0xFF9B7FE8)
)

// endregion
