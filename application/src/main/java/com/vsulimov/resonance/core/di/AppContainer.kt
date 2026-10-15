package com.vsulimov.resonance.core.di

import android.content.Context
import androidx.room.Room
import com.vsulimov.libsubsonic.SubsonicClient
import com.vsulimov.resonance.core.Constants
import com.vsulimov.resonance.core.crypto.KeystoreCryptoManager
import com.vsulimov.resonance.core.imageloader.SubsonicImageLoader
import com.vsulimov.resonance.data.credentials.CredentialsRepository
import com.vsulimov.resonance.data.db.ResonanceDatabase
import com.vsulimov.resonance.data.download.DownloadForegroundService
import com.vsulimov.resonance.data.repository.DefaultDownloadRepository
import com.vsulimov.resonance.data.repository.DefaultFavoritesRepository
import com.vsulimov.resonance.data.repository.DefaultOfflineRepository
import com.vsulimov.resonance.data.settings.SettingsRepository
import com.vsulimov.resonance.data.storage.OfflineStorageManager
import com.vsulimov.resonance.domain.repository.DownloadRepository
import com.vsulimov.resonance.domain.repository.FavoritesRepository
import com.vsulimov.resonance.domain.repository.OfflineRepository
import com.vsulimov.resonance.player.DefaultPlayerController
import com.vsulimov.resonance.player.PlayerController

/**
 * Manual Dependency Injection Container.
 * This interface exposes dependencies that are required across the application.
 */
interface AppContainer {
    /** Repository to securely store and access Subsonic server credentials. */
    val credentialsRepository: CredentialsRepository

    /** Repository to store and manage user preference settings. */
    val settingsRepository: SettingsRepository

    /** Main Room database for offline metadata, relationships, and download queue. */
    val database: ResonanceDatabase

    /** Manager for offline audio files, offline cover art, and local disk space. */
    val offlineStorageManager: OfflineStorageManager

    /** Repository for querying offline data from local Room database. */
    val offlineRepository: OfflineRepository

    /** Repository for managing offline downloads, download queue, and batch resolvers. */
    val downloadRepository: DownloadRepository

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
    val imageLoader: SubsonicImageLoader

    /** Controller for managing audio playback and queue. */
    val playerController: PlayerController

    /** Repository for managing favorite (starred) songs, albums, and artists. */
    val favoritesRepository: FavoritesRepository

    /**
     * Clears all active user session state, stops audio playback, clears caches, and resets saved credentials.
     */
    fun clearSession()
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

    override val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(context)
    }

    override val database: ResonanceDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            ResonanceDatabase::class.java,
            "resonance.db"
        ).fallbackToDestructiveMigration(dropAllTables = true).build()
    }

    override val offlineStorageManager: OfflineStorageManager by lazy {
        OfflineStorageManager(context)
    }

    override val downloadRepository: DownloadRepository by lazy {
        DefaultDownloadRepository(
            context = context,
            database = database,
            offlineStorageManager = offlineStorageManager,
            settingsRepository = settingsRepository,
            getClient = { getAuthenticatedSubsonicClient() }
        )
    }

    override fun createSubsonicClient(baseUrl: String): SubsonicClient =
        SubsonicClient(baseUrl = baseUrl, clientName = Constants.APP_CLIENT_NAME)

    /** Thread-safe cached reference to the currently authenticated [SubsonicClient]. */
    @Volatile
    private var cachedClient: SubsonicClient? = null

    /** Cached server URL used to validate the current [cachedClient] session. */
    private var cachedServerUrl: String? = null

    /** Cached username used to validate the current [cachedClient] session. */
    private var cachedUsername: String? = null

    /** Cached plain-text password used to validate the current [cachedClient] session. */
    private var cachedPasswordPlain: String? = null

    @Synchronized
    override fun getAuthenticatedSubsonicClient(): SubsonicClient? {
        val creds = credentialsRepository.getCredentials() ?: return null

        if ((cachedClient != null) && (cachedServerUrl == creds.serverUrl) &&
            (cachedUsername == creds.username) && (cachedPasswordPlain == creds.passwordPlain)
        ) {
            return cachedClient
        }

        val client = createSubsonicClient(creds.serverUrl)
        client.setCredentials(creds.username, creds.passwordPlain)

        cachedClient = client
        cachedServerUrl = creds.serverUrl
        cachedUsername = creds.username
        cachedPasswordPlain = creds.passwordPlain

        return client
    }

    override val imageLoader: SubsonicImageLoader by lazy {
        SubsonicImageLoader(context, offlineStorageManager) {
            getAuthenticatedSubsonicClient()
        }
    }

    override val playerController: PlayerController by lazy {
        DefaultPlayerController(
            context = context,
            offlineStorageManager = offlineStorageManager,
            settingsRepository = settingsRepository,
            getSubsonicClient = { getAuthenticatedSubsonicClient() }
        )
    }

    override val offlineRepository: OfflineRepository by lazy {
        DefaultOfflineRepository(database)
    }

    override val favoritesRepository: FavoritesRepository by lazy {
        DefaultFavoritesRepository(
            settingsRepository = settingsRepository,
            getSubsonicClient = { getAuthenticatedSubsonicClient() }
        )
    }

    override fun clearSession() {
        DownloadForegroundService.cancelAll(context)
        playerController.stop()
        imageLoader.clearCache()
        favoritesRepository.clearFavorites()
        credentialsRepository.clearCredentials()

        synchronized(this) {
            cachedClient = null
            cachedServerUrl = null
            cachedUsername = null
            cachedPasswordPlain = null
        }
    }
}
