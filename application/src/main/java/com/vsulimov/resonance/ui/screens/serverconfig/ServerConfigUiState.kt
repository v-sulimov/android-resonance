package com.vsulimov.resonance.ui.screens.serverconfig

import com.vsulimov.resonance.core.Constants

/**
 * State representing the Server Configuration Screen.
 */
data class ServerConfigUiState(
    val urlProtocol: String = Constants.PROTOCOL_HTTPS,
    val serverHost: String = "",
    val status: ConnectionStatus = ConnectionStatus.Idle
)
