package com.vsulimov.resonance.domain.usecase

import com.vsulimov.resonance.domain.model.ServerCredentials
import com.vsulimov.resonance.domain.repository.CredentialsRepository

/**
 * Loads previously stored server credentials from secure storage.
 *
 * Used at application startup to determine whether the user has
 * already completed onboarding, and by the main shell to retrieve
 * the username for the avatar display.
 *
 * @param credentialsRepository Repository for credential persistence.
 */
class LoadCredentialsUseCase(
    private val credentialsRepository: CredentialsRepository
) {

    /**
     * Returns the stored [ServerCredentials], or `null` if no credentials
     * have been saved (i.e. the user has not completed onboarding).
     */
    suspend operator fun invoke(): ServerCredentials? = credentialsRepository.load()
}
