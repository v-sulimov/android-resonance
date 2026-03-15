package com.vsulimov.resonance.ui.screen.onboarding.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vsulimov.resonance.ResonanceApplication
import com.vsulimov.resonance.domain.usecase.ConnectToServerUseCase

/**
 * ViewModel scoping [ServerLoginStateHolder] to survive configuration changes.
 *
 * Wires the [ConnectToServerUseCase] into the state holder's connection
 * lambda and exposes a [connect] function that uses [viewModelScope] to
 * ensure the network call survives recomposition.
 *
 * @param connectToServerUseCase Use case that pings the server and persists
 *   credentials on success.
 */
class ServerLoginViewModel(
    private val connectToServerUseCase: ConnectToServerUseCase
) : ViewModel() {

    /** State holder managing form field values, validation, and connection lifecycle. */
    val stateHolder = ServerLoginStateHolder(
        connectToServer = { url, username, password ->
            connectToServerUseCase(url, username, password)
        }
    )

    /**
     * Validates the URL and initiates a connection attempt.
     *
     * Uses [viewModelScope] so the coroutine survives configuration changes
     * and is cancelled only when the ViewModel is cleared.
     */
    fun connect() {
        stateHolder.connect(viewModelScope)
    }

    companion object {

        /**
         * [ViewModelProvider.Factory] that resolves [ConnectToServerUseCase]
         * from the [AppContainer][com.vsulimov.resonance.di.AppContainer]
         * held by [ResonanceApplication].
         */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as ResonanceApplication
                ServerLoginViewModel(
                    connectToServerUseCase = application.appContainer.connectToServerUseCase
                )
            }
        }
    }
}
