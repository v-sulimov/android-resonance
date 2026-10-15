package com.vsulimov.resonance.ui.screens.auth

/**
 * State representing the Authentication Screen.
 */
data class AuthUiState(
    val username: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val status: AuthStatus = AuthStatus.Idle
)
