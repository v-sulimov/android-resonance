package com.vsulimov.resonance.ui.screen.settings

import com.vsulimov.resonance.domain.model.AudioQuality
import com.vsulimov.resonance.domain.model.ThemePreference
import com.vsulimov.resonance.domain.repository.CoverArtRepository
import com.vsulimov.resonance.domain.repository.PreferencesRepository
import com.vsulimov.resonance.domain.usecase.LoadCredentialsUseCase
import com.vsulimov.resonance.domain.usecase.LogoutUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the settings screen.
 *
 * All fields are loaded asynchronously from the credentials and preferences
 * repositories. [Loading] is shown while the initial data fetch is in progress.
 */
sealed class SettingsScreenState {

    /** Initial state while settings data is being loaded. */
    data object Loading : SettingsScreenState()

    /**
     * Settings data loaded and ready for display.
     *
     * @property username The authenticated user's name.
     * @property serverUrl The connected server's base URL.
     * @property serverVersion Server version string, or `null` if unavailable.
     * @property serverType Server type identifier (e.g. "navidrome"), or `null`.
     * @property themePreference Current theme preference.
     * @property audioQuality Current audio quality preference.
     * @property cacheSizeFormatted Human-readable cache size string.
     * @property appVersion Application version string (e.g. "1.0.0").
     */
    data class Content(
        val username: String,
        val serverUrl: String,
        val serverVersion: String?,
        val serverType: String?,
        val themePreference: ThemePreference,
        val audioQuality: AudioQuality,
        val cacheSizeFormatted: String,
        val appVersion: String
    ) : SettingsScreenState()
}

/**
 * State holder for the settings screen.
 *
 * Manages loading of settings data and handles user actions such as
 * changing the theme, audio quality, and disconnecting from the server.
 *
 * This class is not a ViewModel — it is created by [SettingsViewModel]
 * which provides the coroutine scope and lifecycle management.
 *
 * @param loadCredentialsUseCase Loads stored server credentials.
 * @param logoutUseCase Clears credentials and signals session expiry.
 * @param preferencesRepository Persists user preferences and server metadata.
 * @param coverArtRepository Provides cache size information.
 * @param appVersion Application version string from the package info.
 * @param formatCacheSize Formats a byte count as a human-readable string.
 * @param scope Coroutine scope tied to the owning ViewModel's lifecycle.
 */
class SettingsStateHolder(
    private val loadCredentialsUseCase: LoadCredentialsUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val preferencesRepository: PreferencesRepository,
    private val coverArtRepository: CoverArtRepository,
    private val appVersion: String,
    private val formatCacheSize: (Long) -> String,
    private val scope: CoroutineScope
) {

    private val _screenState =
        MutableStateFlow<SettingsScreenState>(SettingsScreenState.Loading)

    /** Current UI state of the settings screen. */
    val screenState: StateFlow<SettingsScreenState> = _screenState.asStateFlow()

    init {
        scope.launch { loadSettings() }
    }

    /**
     * Updates the theme preference.
     *
     * Persists the change and updates the screen state immediately.
     */
    fun setThemePreference(preference: ThemePreference) {
        updateContent { it.copy(themePreference = preference) }
        scope.launch { preferencesRepository.setThemePreference(preference) }
    }

    /**
     * Updates the audio quality preference.
     *
     * Persists the change and updates the screen state with the new
     * summary text.
     */
    fun setAudioQuality(quality: AudioQuality) {
        updateContent { it.copy(audioQuality = quality) }
        scope.launch { preferencesRepository.setAudioQuality(quality) }
    }

    /**
     * Disconnects from the server by clearing all stored data
     * and signaling session expiry.
     */
    fun disconnect() {
        scope.launch { logoutUseCase() }
    }

    /**
     * Refreshes the displayed cache size.
     *
     * Called when returning from the cache management screen
     * where the user may have cleared the cache.
     */
    fun refreshCacheSize() {
        updateContent {
            it.copy(cacheSizeFormatted = formatCacheSize(coverArtRepository.getCacheSizeBytes()))
        }
    }

    private suspend fun loadSettings() {
        val credentials = loadCredentialsUseCase()
        val audioQuality = preferencesRepository.getAudioQuality()
        val serverVersion = preferencesRepository.getServerVersion()
        val serverType = preferencesRepository.getServerType()
        val themePreference = preferencesRepository.themePreference.value
        val cacheSize = coverArtRepository.getCacheSizeBytes()

        _screenState.value = SettingsScreenState.Content(
            username = credentials?.username ?: "",
            serverUrl = credentials?.serverUrl ?: "",
            serverVersion = serverVersion,
            serverType = serverType,
            themePreference = themePreference,
            audioQuality = audioQuality,
            cacheSizeFormatted = formatCacheSize(cacheSize),
            appVersion = appVersion
        )
    }

    private fun updateContent(transform: (SettingsScreenState.Content) -> SettingsScreenState.Content) {
        val current = _screenState.value
        if (current is SettingsScreenState.Content) {
            _screenState.value = transform(current)
        }
    }
}
