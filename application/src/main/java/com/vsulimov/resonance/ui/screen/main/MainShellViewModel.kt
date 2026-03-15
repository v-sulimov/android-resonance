package com.vsulimov.resonance.ui.screen.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vsulimov.resonance.ResonanceApplication
import com.vsulimov.resonance.domain.usecase.LoadCredentialsUseCase

/**
 * ViewModel for the main application shell.
 *
 * Scopes the [MainShellStateHolder] to the ViewModel lifecycle so that
 * the selected tab and avatar initial survive configuration changes.
 *
 * @param loadCredentialsUseCase Use case for loading stored credentials.
 */
class MainShellViewModel(
    loadCredentialsUseCase: LoadCredentialsUseCase
) : ViewModel() {

    /** State holder managing tab selection and avatar initial. */
    val stateHolder = MainShellStateHolder(
        loadCredentialsUseCase = loadCredentialsUseCase,
        scope = viewModelScope
    )

    companion object {

        /** Factory that resolves dependencies from [ResonanceApplication.appContainer]. */
        val Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as ResonanceApplication
                MainShellViewModel(
                    loadCredentialsUseCase = application.appContainer.loadCredentialsUseCase
                )
            }
        }
    }
}
