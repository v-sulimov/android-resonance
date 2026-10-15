package com.vsulimov.resonance.data.settings

import androidx.annotation.StringRes
import com.vsulimov.resonance.R

/**
 * Enumerates the supported application theme display modes.
 *
 * @property titleResId String resource ID for the localized theme mode label.
 */
enum class AppThemeMode(@StringRes val titleResId: Int) {
    SYSTEM(R.string.settings_theme_system),
    LIGHT(R.string.settings_theme_light),
    DARK(R.string.settings_theme_dark)
}
