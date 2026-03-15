package com.vsulimov.resonance.domain.model

/**
 * User preference for the application theme.
 *
 * Controls which color scheme is applied by
 * [ResonanceTheme][com.vsulimov.resonance.ui.theme.ResonanceTheme].
 */
enum class ThemePreference {

    /** Follows the system-wide dark/light setting. */
    SYSTEM,

    /** Always use the light color scheme. */
    LIGHT,

    /** Always use the dark color scheme. */
    DARK
}
