package com.vsulimov.resonance.core.di

import android.content.Context
import com.vsulimov.libsubsonic.SubsonicClient
import com.vsulimov.resonance.core.Constants
import com.vsulimov.resonance.core.crypto.KeystoreCryptoManager
import com.vsulimov.resonance.data.credentials.CredentialsRepository

/**
 * Manual Dependency Injection Container.
 * This interface exposes dependencies that are required across the application.
 */
interface AppContainer {
    /** Repository to securely store and access Subsonic server credentials. */
    val credentialsRepository: CredentialsRepository

    /**
     * Creates a new instance of [SubsonicClient].
     *
     * @param baseUrl The server URL to connect to.
     * @return A configured [SubsonicClient].
     */
    fun createSubsonicClient(baseUrl: String): SubsonicClient

    /**
     * Creates or returns a configured [SubsonicClient] using the saved credentials.
     * @return A configured [SubsonicClient] if credentials exist, otherwise null.
     */
    fun getAuthenticatedSubsonicClient(): SubsonicClient?

    /** The global image loader for cover arts. */
    val imageLoader: com.vsulimov.resonance.core.imageloader.SubsonicImageLoader
}

/**
 * Default implementation of the [AppContainer].
 * Instantiates and holds singleton instances of the core dependencies.
 */
class DefaultAppContainer(private val context: Context) : AppContainer {

    /** Provides the cryptographic logic for encrypting user passwords. */
    private val cryptoManager by lazy {
        KeystoreCryptoManager()
    }

    override val credentialsRepository: CredentialsRepository by lazy {
        CredentialsRepository(context, cryptoManager)
    }

    override fun createSubsonicClient(baseUrl: String): SubsonicClient =
        SubsonicClient(baseUrl = baseUrl, clientName = Constants.APP_CLIENT_NAME)

    override fun getAuthenticatedSubsonicClient(): SubsonicClient? {
        val creds = credentialsRepository.getCredentials() ?: return null
        val client = createSubsonicClient(creds.serverUrl)
        client.setCredentials(creds.username, creds.passwordPlain)
        return client
    }

    override val imageLoader by lazy {
        com.vsulimov.resonance.core.imageloader.SubsonicImageLoader(context) {
            getAuthenticatedSubsonicClient()
        }
    }
}
