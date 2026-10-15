package com.vsulimov.resonance.ui.screens.serverconfig

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vsulimov.libsubsonic.SubsonicClient
import com.vsulimov.libsubsonic.data.result.SubsonicResult
import com.vsulimov.resonance.R
import com.vsulimov.resonance.core.Constants
import com.vsulimov.resonance.core.di.AppContainer
import com.vsulimov.resonance.core.ui.appContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Represents the current status of the server connection test.
 */
sealed class ConnectionStatus {
    object Idle : ConnectionStatus()
    object Testing : ConnectionStatus()
    data class Success(val serverVersion: String) : ConnectionStatus()
    data class Error(val messageResId: Int) : ConnectionStatus()
}

/**
 * ViewModel responsible for managing the state and logic of the Server Configuration screen.
 *
 * @property createSubsonicClient A factory function to instantiate a [SubsonicClient].
 */
class ServerConfigViewModel(private val createSubsonicClient: (String) -> SubsonicClient) : ViewModel() {

    private val _urlProtocol = MutableStateFlow(Constants.PROTOCOL_HTTPS)

    /** The selected protocol (http:// or https://) */
    val urlProtocol: StateFlow<String> = _urlProtocol.asStateFlow()

    private val _serverHost = MutableStateFlow("")

    /** The current server host or IP inputted by the user. */
    val serverHost: StateFlow<String> = _serverHost.asStateFlow()

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Idle)

    /** The current state of the connection test. */
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    /**
     * Updates the URL protocol (http/https).
     */
    fun onProtocolChanged(protocol: String) {
        _urlProtocol.value = protocol
        resetStatus()
    }

    /**
     * Updates the server host state. Strips prefixes if the user accidentally pastes a full URL.
     *
     * @param host The new host string entered by the user.
     */
    fun onServerHostChanged(host: String) {
        var cleanedHost = host.trim()

        // Auto-detect and fix protocol if user pasted a full URL
        if (cleanedHost.startsWith(Constants.PROTOCOL_HTTPS, ignoreCase = true)) {
            _urlProtocol.value = Constants.PROTOCOL_HTTPS
            cleanedHost = cleanedHost.substring(Constants.PROTOCOL_HTTPS.length)
        } else if (cleanedHost.startsWith(Constants.PROTOCOL_HTTP, ignoreCase = true)) {
            _urlProtocol.value = Constants.PROTOCOL_HTTP
            cleanedHost = cleanedHost.substring(Constants.PROTOCOL_HTTP.length)
        }

        _serverHost.value = cleanedHost
        resetStatus()
    }

    private fun resetStatus() {
        if (_connectionStatus.value !is ConnectionStatus.Testing) {
            _connectionStatus.value = ConnectionStatus.Idle
        }
    }

    /** The full URL derived from the protocol and host */
    private val fullUrl: String
        get() = "${_urlProtocol.value}${_serverHost.value.trim()}"

    /**
     * Initiates a connection test to the current full URL.
     *
     * Validates the URL format before launching a coroutine to ping the server.
     * Updates the [connectionStatus] based on the result.
     */
    fun testConnection() {
        val currentUrl = fullUrl

        if (_serverHost.value.isBlank()) {
            _connectionStatus.value = ConnectionStatus.Error(R.string.status_error_invalid_url)
            return
        }

        _connectionStatus.value = ConnectionStatus.Testing

        viewModelScope.launch {
            try {
                val version = withContext(Dispatchers.IO) {
                    val client = createSubsonicClient(currentUrl)
                    when (val result = client.ping()) {
                        is SubsonicResult.Success -> {
                            result.data.serverVersion ?: result.data.apiVersion
                        }

                        is SubsonicResult.Failure -> {
                            throw Exception(result.error.message)
                        }
                    }
                }

                _connectionStatus.value = ConnectionStatus.Success(version)
            } catch (_: Exception) {
                _connectionStatus.value = ConnectionStatus.Error(R.string.status_error_connection)
            }
        }
    }

    /**
     * Clears the current host input.
     */
    fun clearServerHost() {
        _serverHost.value = ""
        resetStatus()
    }

    companion object {
        /** Factory to instantiate [ServerConfigViewModel] with dependencies from the [AppContainer]. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val appContainer = this.appContainer
                ServerConfigViewModel(
                    createSubsonicClient = { baseUrl -> appContainer.createSubsonicClient(baseUrl) }
                )
            }
        }
    }
}
