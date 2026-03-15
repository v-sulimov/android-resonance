package com.vsulimov.resonance.domain.repository

import com.vsulimov.resonance.domain.model.ServerCredentials

/**
 * Abstraction for persisting and retrieving server credentials.
 *
 * Implementations must store credentials securely (e.g. using encrypted
 * storage) since they contain a plain-text password.
 */
interface CredentialsRepository {

    /**
     * Persists the given [credentials] to secure storage.
     *
     * Any previously stored credentials are overwritten.
     *
     * @param credentials The server URL, username, and password to store.
     */
    suspend fun save(credentials: ServerCredentials)

    /**
     * Loads previously stored credentials.
     *
     * @return The stored [ServerCredentials], or `null` if no credentials
     *   have been saved yet.
     */
    suspend fun load(): ServerCredentials?

    /**
     * Removes all stored credentials from secure storage.
     */
    suspend fun clear()
}
