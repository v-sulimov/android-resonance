package com.vsulimov.resonance.data.remote

import com.vsulimov.libsubsonic.SubsonicClient
import com.vsulimov.resonance.domain.repository.ClientInvalidator
import com.vsulimov.resonance.domain.repository.CredentialsRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Lazily creates and caches a [SubsonicClient] configured with stored credentials.
 *
 * The client instance is created on first access and reused for all subsequent
 * API calls. Thread safety is ensured via a [Mutex] to prevent duplicate client
 * creation from concurrent coroutines.
 *
 * Call [invalidate] when credentials change (e.g. logout) to force a fresh
 * client on the next access.
 *
 * @param credentialsRepository Source of stored server URL and authentication details.
 */
class SubsonicClientProvider(
    private val credentialsRepository: CredentialsRepository
) : ClientInvalidator {

    private val mutex = Mutex()

    @Volatile
    private var cachedClient: SubsonicClient? = null

    /**
     * Returns a configured [SubsonicClient], creating one if necessary.
     *
     * On first call, loads credentials from [credentialsRepository], creates
     * a [SubsonicClient] with the stored server URL, and sets the authentication
     * details. Subsequent calls return the cached instance.
     *
     * @return A ready-to-use [SubsonicClient], or `null` if no credentials
     *   are stored (i.e. the user has not completed onboarding).
     */
    suspend fun getClient(): SubsonicClient? {
        cachedClient?.let { return it }
        return mutex.withLock {
            cachedClient?.let { return it }
            val credentials = credentialsRepository.load() ?: return null
            val client = SubsonicClient(baseUrl = credentials.serverUrl)
            client.setCredentials(
                username = credentials.username,
                password = credentials.password
            )
            cachedClient = client
            client
        }
    }

    /**
     * Discards the cached client so that the next [getClient] call
     * creates a fresh instance with current credentials.
     */
    override fun invalidate() {
        cachedClient = null
    }
}
