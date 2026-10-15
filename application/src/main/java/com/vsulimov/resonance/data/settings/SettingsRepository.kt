package com.vsulimov.resonance.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository responsible for storing and managing user preference settings.
 * Uses [SharedPreferences] for persistent storage.
 *
 * @param context The application context used to retrieve [SharedPreferences].
 */
class SettingsRepository(context: Context) {

    companion object {
        private const val PREFS_NAME = "resonance_settings"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color_enabled"
        private const val KEY_OFFLINE_MODE = "offline_mode_enabled"
        private const val KEY_DOWNLOAD_QUEUE_PAUSED = "download_queue_paused"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val _themeModeState = MutableStateFlow(loadThemeMode())

    /** State flow reflecting the current app theme selection mode. */
    val themeModeState: StateFlow<AppThemeMode> = _themeModeState.asStateFlow()

    private val _isDynamicColorEnabledState = MutableStateFlow(loadDynamicColorEnabled())

    /** State flow reflecting whether Material You dynamic color is enabled. */
    val isDynamicColorEnabledState: StateFlow<Boolean> = _isDynamicColorEnabledState.asStateFlow()

    private val _isOfflineModeEnabledState = MutableStateFlow(loadOfflineModeEnabled())

    /** State flow reflecting whether strict offline mode is active. */
    val isOfflineModeEnabledState: StateFlow<Boolean> = _isOfflineModeEnabledState.asStateFlow()

    private val _isDownloadQueuePausedState = MutableStateFlow(loadDownloadQueuePaused())

    /** State flow reflecting whether active download queue processing is paused. */
    val isDownloadQueuePausedState: StateFlow<Boolean> = _isDownloadQueuePausedState.asStateFlow()

    /**
     * Updates the selected application theme mode.
     *
     * @param mode Selected [AppThemeMode].
     */
    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit { putString(KEY_THEME_MODE, mode.name) }
        _themeModeState.value = mode
    }

    /**
     * Updates whether Material You dynamic color scheme is enabled.
     *
     * @param enabled True to enable dynamic color, false otherwise.
     */
    fun setDynamicColorEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_DYNAMIC_COLOR, enabled) }
        _isDynamicColorEnabledState.value = enabled
    }

    /**
     * Updates whether strict offline mode is enabled.
     *
     * @param enabled True to enable offline mode, false otherwise.
     */
    fun setOfflineModeEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_OFFLINE_MODE, enabled) }
        _isOfflineModeEnabledState.value = enabled
    }

    /**
     * Updates whether the download queue worker is paused.
     *
     * @param paused True if queue is paused, false otherwise.
     */
    fun setDownloadQueuePaused(paused: Boolean) {
        prefs.edit { putBoolean(KEY_DOWNLOAD_QUEUE_PAUSED, paused) }
        _isDownloadQueuePausedState.value = paused
    }

    /**
     * Synchronously checks if offline mode is enabled.
     *
     * @return True if offline mode is active.
     */
    fun isOfflineModeEnabledSync(): Boolean = prefs.getBoolean(KEY_OFFLINE_MODE, false)

    /**
     * Resets all user preference settings back to default values.
     */
    fun clearSettings() {
        prefs.edit { clear() }
        _themeModeState.value = AppThemeMode.SYSTEM
        _isDynamicColorEnabledState.value = true
        _isOfflineModeEnabledState.value = false
        _isDownloadQueuePausedState.value = false
    }

    /** Loads saved theme display mode from preferences with fallback to system default. */
    private fun loadThemeMode(): AppThemeMode {
        val savedName = prefs.getString(KEY_THEME_MODE, null) ?: return AppThemeMode.SYSTEM
        return try {
            AppThemeMode.valueOf(savedName)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            AppThemeMode.SYSTEM
        }
    }

    /** Loads saved dynamic color state from preferences with fallback to true. */
    private fun loadDynamicColorEnabled(): Boolean = prefs.getBoolean(KEY_DYNAMIC_COLOR, true)

    /** Loads saved offline mode toggle from preferences with fallback to false. */
    private fun loadOfflineModeEnabled(): Boolean = prefs.getBoolean(KEY_OFFLINE_MODE, false)

    /** Loads saved download queue paused state from preferences with fallback to false. */
    private fun loadDownloadQueuePaused(): Boolean = prefs.getBoolean(KEY_DOWNLOAD_QUEUE_PAUSED, false)
}
