package com.vsulimov.resonance.domain.usecase

import com.vsulimov.resonance.core.SessionStateHolder
import com.vsulimov.resonance.domain.repository.ClientInvalidator
import com.vsulimov.resonance.domain.repository.CredentialsRepository
import com.vsulimov.resonance.domain.repository.PreferencesRepository

/**
 * Clears all stored credentials and preferences, discards the cached
 * API client, and notifies the application that the current session
 * has ended.
 *
 * This use case is invoked in two situations:
 * 1. **Authentication failure** — the server rejects stored credentials
 *    (e.g. the password was changed remotely).
 * 2. **User-initiated logout** — the user explicitly signs out from settings.
 *
 * After invocation, the [SessionStateHolder] emits a session-expired event
 * which the top-level navigation observes to redirect to onboarding.
 *
 * @param credentialsRepository Repository for credential persistence.
 * @param preferencesRepository Repository for user preferences and server metadata.
 * @param sessionStateHolder Application-wide session lifecycle holder.
 * @param clientInvalidator Invalidates the cached API client instance.
 */
class LogoutUseCase(
    private val credentialsRepository: CredentialsRepository,
    private val preferencesRepository: PreferencesRepository,
    private val sessionStateHolder: SessionStateHolder,
    private val clientInvalidator: ClientInvalidator
) {

    /**
     * Clears stored credentials and preferences, invalidates the cached
     * client, and signals session expiry.
     */
    suspend operator fun invoke() {
        credentialsRepository.clear()
        preferencesRepository.clear()
        clientInvalidator.invalidate()
        sessionStateHolder.notifySessionExpired()
    }
}
