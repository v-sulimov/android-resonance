package com.vsulimov.resonance.ui.screens.settings

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vsulimov.resonance.BuildConfig
import com.vsulimov.resonance.R
import com.vsulimov.resonance.core.di.AppContainer
import com.vsulimov.resonance.core.imageloader.SubsonicImageLoader
import com.vsulimov.resonance.core.ui.UiText
import com.vsulimov.resonance.core.ui.appContainer
import com.vsulimov.resonance.data.credentials.CredentialsRepository
import com.vsulimov.resonance.data.settings.AppThemeMode
import com.vsulimov.resonance.data.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing application preferences, theme settings, image cache, and user sign-out.
 * @property appContainer Manual DI container instance for managing global session state.
 * @property credentialsRepository Repository for managing saved credentials.
 * @property settingsRepository Repository for managing app theme and dynamic color preferences.
 * @property imageLoader Image loader instance for computing and clearing image disk cache.
 */
class SettingsViewModel(
    private val appContainer: AppContainer,
    private val credentialsRepository: CredentialsRepository,
    private val settingsRepository: SettingsRepository,
    private val imageLoader: SubsonicImageLoader
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())

    /** Exposes immutable UI state for the Settings screen. */
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        observeThemePreferences()
    }

    /** Toggles visibility of the theme selection bottom sheet. */
    fun toggleThemeBottomSheet() {
        _uiState.update { it.copy(isThemeBottomSheetOpen = !it.isThemeBottomSheetOpen) }
    }

    /** Updates selected theme display mode and closes the theme bottom sheet. */
    fun onThemeModeChanged(mode: AppThemeMode) {
        settingsRepository.setThemeMode(mode)
        _uiState.update { it.copy(isThemeBottomSheetOpen = false) }
    }

    /** Toggles Material You dynamic color scheme. */
    fun onDynamicColorToggled(enabled: Boolean) {
        settingsRepository.setDynamicColorEnabled(enabled)
    }

    /** Clears cover art memory and disk cache and updates current cache size. */
    fun onClearCacheClick() {
        viewModelScope.launch {
            imageLoader.clearCache()
            val newSizeBytes = imageLoader.getCacheSizeBytes()
            _uiState.update {
                it.copy(
                    cacheSizeBytes = newSizeBytes,
                    userMessage = UiText.StringResource(R.string.settings_cache_cleared_message)
                )
            }
        }
    }

    /** Resets transient user feedback message after display. */
    fun onUserMessageShown() {
        _uiState.update { it.copy(userMessage = null) }
    }

    /** Shows sign-out confirmation dialog. */
    fun onSignOutClick() {
        _uiState.update { it.copy(isSignOutDialogVisible = true) }
    }

    /** Dismisses sign-out confirmation dialog. */
    fun onDismissSignOutDialog() {
        _uiState.update { it.copy(isSignOutDialogVisible = false) }
    }

    /** Resets all user settings and credentials, and executes callback to return to onboarding. */
    fun onConfirmSignOut(onLoggedOut: () -> Unit) {
        _uiState.update { it.copy(isSignOutDialogVisible = false) }
        appContainer.clearSession()
        onLoggedOut()
    }

    /** Loads saved credentials, cache stats, and SDK feature flags. */
    private fun loadSettings() {
        val creds = credentialsRepository.getCredentials()
        val cacheSizeBytes = imageLoader.getCacheSizeBytes()
        val isDynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

        _uiState.update {
            it.copy(
                username = creds?.username ?: "",
                serverUrl = creds?.serverUrl ?: "",
                cacheSizeBytes = cacheSizeBytes,
                appVersion = BuildConfig.VERSION_NAME,
                isDynamicColorSupported = isDynamicSupported
            )
        }
    }

    /** Subscribes to reactive theme and dynamic color state flows from [SettingsRepository]. */
    private fun observeThemePreferences() {
        viewModelScope.launch {
            settingsRepository.themeModeState.collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }

        viewModelScope.launch {
            settingsRepository.isDynamicColorEnabledState.collect { enabled ->
                _uiState.update { it.copy(isDynamicColorEnabled = enabled) }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val appContainer = this.appContainer
                SettingsViewModel(
                    appContainer = appContainer,
                    credentialsRepository = appContainer.credentialsRepository,
                    settingsRepository = appContainer.settingsRepository,
                    imageLoader = appContainer.imageLoader
                )
            }
        }
    }
}
