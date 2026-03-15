package com.vsulimov.resonance.ui.screen.onboarding.login

import com.vsulimov.resonance.domain.model.ServerConnectionInfo
import com.vsulimov.resonance.ui.mapper.ConnectionSuccessMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Represents the current state of the server connection attempt.
 *
 * The lifecycle progresses as: [Idle] -> [Loading] -> [Success] or [Error].
 * Editing any form field while in [Error] or [Success] resets back to [Idle].
 */
sealed class ConnectionState {

    /** No connection attempt has been made, or the previous result was dismissed. */
    data object Idle : ConnectionState()

    /** A connection attempt is in progress. */
    data object Loading : ConnectionState()

    /**
     * The connection attempt failed.
     *
     * @property message User-facing error description. May be empty if the
     *   underlying exception had no message.
     */
    data class Error(val message: String) : ConnectionState()

    /**
     * The connection attempt succeeded.
     *
     * @property viewObject Presentation-ready data for display in the UI.
     */
    data class Success(val viewObject: ConnectionSuccessViewObject) : ConnectionState()
}

/**
 * State holder for the server login screen.
 *
 * Manages form field values, inline URL validation, password visibility,
 * and the connection lifecycle. The actual network call is delegated to
 * [connectToServer], allowing the data layer to be swapped or stubbed.
 *
 * This class is not a ViewModel — it is created by
 * [ServerLoginViewModel] and receives a [CoroutineScope] from the caller
 * for launching coroutines.
 *
 * @param connectToServer Suspend function that attempts to authenticate with a server.
 *   Receives server URL, username, and password. Returns [Result] wrapping
 *   [ServerConnectionInfo].
 */
class ServerLoginStateHolder(
    private val connectToServer:
    suspend (url: String, username: String, password: String) -> Result<ServerConnectionInfo>
) {

    companion object {
        /** Pattern matching a URL starting with http:// or https:// followed by non-whitespace. */
        private val URL_PATTERN = Regex("^https?://\\S+$")

        /**
         * Minimum duration in milliseconds to display the loading state.
         *
         * Prevents a visual flicker when the server responds faster than
         * the spinner animation can complete a meaningful cycle.
         */
        private const val MIN_LOADING_DURATION_MS = 500L
    }

    private val _serverUrl = MutableStateFlow("")

    /** Current value of the server URL text field. */
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private val _username = MutableStateFlow("")

    /** Current value of the username text field. */
    val username: StateFlow<String> = _username.asStateFlow()

    private val _password = MutableStateFlow("")

    /** Current value of the password text field. */
    val password: StateFlow<String> = _password.asStateFlow()

    private val _urlValidationFailed = MutableStateFlow(false)

    /** Whether the server URL field is currently showing a validation error. */
    val urlValidationFailed: StateFlow<Boolean> = _urlValidationFailed.asStateFlow()

    private val _passwordVisible = MutableStateFlow(false)

    /** Whether the password field text is currently visible (not masked). */
    val passwordVisible: StateFlow<Boolean> = _passwordVisible.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)

    /** Current state of the server connection attempt. */
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    /** Updates the server URL field and clears any validation or connection errors. */
    fun onServerUrlChanged(value: String) {
        _serverUrl.value = value
        _urlValidationFailed.value = false
        dismissBanner()
    }

    /** Updates the username field and dismisses any active banner. */
    fun onUsernameChanged(value: String) {
        _username.value = value
        dismissBanner()
    }

    /** Updates the password field and dismisses any active banner. */
    fun onPasswordChanged(value: String) {
        _password.value = value
        dismissBanner()
    }

    /** Toggles the password field between masked and plain-text display. */
    fun togglePasswordVisibility() {
        _passwordVisible.value = !_passwordVisible.value
    }

    /**
     * Validates the server URL format.
     *
     * Sets [urlValidationFailed] to `true` if the URL does not match the expected pattern.
     *
     * @return `true` if the URL starts with `http://` or `https://` followed by
     *   non-whitespace characters.
     */
    fun validateUrl(): Boolean {
        val url = _serverUrl.value.trim()
        val valid = url.matches(URL_PATTERN)
        _urlValidationFailed.value = !valid
        return valid
    }

    /**
     * Validates the URL, then attempts to connect to the server.
     *
     * The ping request and a minimum display timer run in parallel so
     * the loading indicator is shown for at least [MIN_LOADING_DURATION_MS],
     * avoiding a visual flicker on fast connections without adding latency
     * to slow ones.
     *
     * Updates [connectionState] through [ConnectionState.Idle] ->
     * [ConnectionState.Loading] -> [ConnectionState.Success] or [ConnectionState.Error].
     *
     * @param scope Coroutine scope used to launch the connection coroutine.
     *   Should be the ViewModel scope so the operation survives recomposition.
     */
    fun connect(scope: CoroutineScope) {
        if (_connectionState.value is ConnectionState.Loading) return
        if (!validateUrl()) return

        scope.launch {
            _connectionState.value = ConnectionState.Loading

            val resultDeferred = async {
                connectToServer(
                    _serverUrl.value.trim(),
                    _username.value.trim(),
                    _password.value
                )
            }
            delay(MIN_LOADING_DURATION_MS)
            val result = resultDeferred.await()

            _connectionState.value = result.fold(
                onSuccess = { info ->
                    ConnectionState.Success(viewObject = ConnectionSuccessMapper.map(info))
                },
                onFailure = { error ->
                    ConnectionState.Error(error.message.orEmpty())
                }
            )
        }
    }

    /**
     * Resets the connection state to [ConnectionState.Idle] if currently
     * showing an error or success banner.
     */
    private fun dismissBanner() {
        val current = _connectionState.value
        if (current is ConnectionState.Error || current is ConnectionState.Success) {
            _connectionState.value = ConnectionState.Idle
        }
    }
}
