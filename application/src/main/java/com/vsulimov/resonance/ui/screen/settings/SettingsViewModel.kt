package com.vsulimov.resonance.ui.screen.settings

import android.text.format.Formatter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vsulimov.resonance.BuildConfig
import com.vsulimov.resonance.ResonanceApplication

/**
 * ViewModel for the settings screen.
 *
 * Scopes the [SettingsStateHolder] to the ViewModel lifecycle so that
 * settings data survives configuration changes.
 *
 * @param application Application reference for formatting utilities.
 */
class SettingsViewModel(
    application: ResonanceApplication
) : ViewModel() {

    /** State holder managing settings data and user actions. */
    val stateHolder = SettingsStateHolder(
        loadCredentialsUseCase = application.appContainer.loadCredentialsUseCase,
        logoutUseCase = application.appContainer.logoutUseCase,
        preferencesRepository = application.appContainer.preferencesRepository,
        coverArtRepository = application.appContainer.coverArtRepository,
        appVersion = BuildConfig.VERSION_NAME,
        formatCacheSize = { bytes -> Formatter.formatShortFileSize(application, bytes) },
        scope = viewModelScope
    )

    companion object {

        /** Factory that resolves dependencies from [ResonanceApplication.appContainer]. */
        val Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as ResonanceApplication
                SettingsViewModel(application = application)
            }
        }
    }
}
