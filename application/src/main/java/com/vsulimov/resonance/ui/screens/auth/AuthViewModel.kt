package com.vsulimov.resonance.ui.screens.auth

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vsulimov.libsubsonic.SubsonicClient
import com.vsulimov.libsubsonic.data.result.SubsonicResult
import com.vsulimov.libsubsonic.data.result.error.SubsonicError
import com.vsulimov.libsubsonic.data.result.error.SubsonicErrorCode
import com.vsulimov.resonance.R
import com.vsulimov.resonance.core.di.AppContainer
import com.vsulimov.resonance.core.ui.appContainer
import com.vsulimov.resonance.data.credentials.CredentialsRepository
import com.vsulimov.resonance.data.credentials.SubsonicCredentials
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Represents the current status of the authentication process.
 */
sealed class AuthStatus {
    object Idle : AuthStatus()
    object Authenticating : AuthStatus()
    object Success : AuthStatus()
    data class Error(val messageResId: Int) : AuthStatus()
}

/**
 * ViewModel responsible for managing the state and logic of the Authentication screen.
 */
class AuthViewModel(
    savedStateHandle: SavedStateHandle,
    private val credentialsRepository: CredentialsRepository,
    private val createSubsonicClient: (String) -> SubsonicClient
) : ViewModel() {

    /** The server URL decoded from the navigation arguments. */
    val serverUrl: String = URLDecoder.decode(
        requireNotNull(savedStateHandle["serverUrl"]) { "serverUrl navigation argument is missing" },
        StandardCharsets.UTF_8.name()
    )

    private val _uiState = MutableStateFlow(AuthUiState())

    /** The current state of the Auth screen. */
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /** Updates the username state and resets any previous error status. */
    fun onUsernameChanged(newUsername: String) {
        _uiState.update { it.copy(username = newUsername) }
        resetStatus()
    }

    /** Updates the password state and resets any previous error status. */
    fun onPasswordChanged(newPassword: String) {
        _uiState.update { it.copy(password = newPassword) }
        resetStatus()
    }

    /** Toggles the visibility state of the password field. */
    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    private fun resetStatus() {
        if (_uiState.value.status !is AuthStatus.Authenticating) {
            _uiState.update { it.copy(status = AuthStatus.Idle) }
        }
    }

    /**
     * Initiates the login process using the provided credentials.
     *
     * Validates that the fields are not empty, configures the [SubsonicClient],
     * and performs a test ping. If successful, saves the credentials securely.
     */
    fun login() {
        val currentUsername = _uiState.value.username.trim()
        val currentPassword = _uiState.value.password

        if (currentUsername.isEmpty() || currentPassword.isEmpty()) {
            _uiState.update { it.copy(status = AuthStatus.Error(R.string.status_error_empty_fields)) }
            return
        }

        _uiState.update { it.copy(status = AuthStatus.Authenticating) }

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val client = createSubsonicClient(serverUrl)
                    client.setCredentials(currentUsername, currentPassword)

                    when (val result = client.ping(useDummyAuthIfMissing = false)) {
                        is SubsonicResult.Success -> {
                            val credentials = SubsonicCredentials(
                                serverUrl = serverUrl,
                                username = currentUsername,
                                passwordPlain = currentPassword
                            )
                            credentialsRepository.saveCredentials(credentials)
                        }

                        is SubsonicResult.Failure -> {
                            throw result.error
                        }
                    }
                }

                _uiState.update { it.copy(status = AuthStatus.Success) }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e

                val isAuthFailure = e is SubsonicError && (
                    e.code == SubsonicErrorCode.WRONG_CREDENTIALS ||
                        e.code == SubsonicErrorCode.WRONG_CREDENTIALS_LEGACY ||
                        e.code == SubsonicErrorCode.NOT_AUTHORIZED
                    )

                if (isAuthFailure) {
                    _uiState.update { it.copy(status = AuthStatus.Error(R.string.status_error_auth_failed)) }
                } else {
                    _uiState.update { it.copy(status = AuthStatus.Error(R.string.status_error_connection)) }
                }
            }
        }
    }

    companion object {
        /** Factory to instantiate [AuthViewModel] with dependencies from the [AppContainer]. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val savedStateHandle = createSavedStateHandle()
                val appContainer = this.appContainer
                AuthViewModel(
                    savedStateHandle = savedStateHandle,
                    credentialsRepository = appContainer.credentialsRepository,
                    createSubsonicClient = { baseUrl -> appContainer.createSubsonicClient(baseUrl) }
                )
            }
        }
    }
}
