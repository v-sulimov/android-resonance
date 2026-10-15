package com.vsulimov.resonance.ui.screens.settings

import com.vsulimov.resonance.core.ui.UiText
import com.vsulimov.resonance.data.settings.AppThemeMode

/**
 * Immutable UI state for the Settings & Profile Screen.
 *
 * @property username Currently logged in Subsonic username.
 * @property serverUrl Connected Subsonic server URL.
 * @property themeMode Selected application theme mode.
 * @property isDynamicColorEnabled Whether Material You dynamic color scheme is enabled.
 * @property isDynamicColorSupported Whether the device supports Material You dynamic color (Android 12+).
 * @property cacheSizeBytes Total disk space used by the cover art image cache in bytes.
 * @property appVersion Application version name string from BuildConfig.
 * @property isThemeBottomSheetOpen Whether the theme selection bottom sheet is currently open.
 * @property isSignOutDialogVisible Whether the sign out confirmation dialog is currently displayed.
 * @property userMessage Transient UI feedback message (e.g. "Image cache cleared").
 */
data class SettingsUiState(
    val username: String = "",
    val serverUrl: String = "",
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val isDynamicColorEnabled: Boolean = true,
    val isDynamicColorSupported: Boolean = true,
    val cacheSizeBytes: Long = 0L,
    val appVersion: String = "",
    val isThemeBottomSheetOpen: Boolean = false,
    val isSignOutDialogVisible: Boolean = false,
    val userMessage: UiText? = null
)
