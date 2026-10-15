package com.vsulimov.resonance.data.credentials

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.vsulimov.resonance.core.crypto.KeystoreCryptoManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository responsible for securely storing and retrieving Subsonic server credentials.
 * Uses standard [SharedPreferences] for basic strings and [KeystoreCryptoManager] for the password.
 *
 * @property context The application context.
 * @property cryptoManager The manager handling AES/GCM/NoPadding encryption.
 */
class CredentialsRepository(private val context: Context, private val cryptoManager: KeystoreCryptoManager) {

    companion object {
        private const val PREFS_NAME = "resonance_credentials"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD_ENCRYPTED = "password_encrypted_base64"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val _credentialsState = MutableStateFlow(loadCredentials())

    /** Observable state flow containing the current [SubsonicCredentials] or null if not logged in. */
    val credentialsState: StateFlow<SubsonicCredentials?> = _credentialsState.asStateFlow()

    /**
     * Checks if a user is currently configured/logged in.
     *
     * @return True if credentials exist, false otherwise.
     */
    fun hasCredentials(): Boolean = _credentialsState.value != null

    /**
     * Retrieves the current credentials synchronously.
     *
     * @return The current [SubsonicCredentials] or null if not logged in.
     */
    fun getCredentials(): SubsonicCredentials? = _credentialsState.value

    /**
     * Encrypts the password and saves all credentials to persistent storage.
     *
     * @param credentials The credentials to save.
     */
    fun saveCredentials(credentials: SubsonicCredentials) {
        val encryptedPasswordBytes = cryptoManager.encrypt(credentials.passwordPlain)
        val base64EncryptedPassword = Base64.encodeToString(encryptedPasswordBytes, Base64.NO_WRAP)

        prefs.edit().apply {
            putString(KEY_SERVER_URL, credentials.serverUrl)
            putString(KEY_USERNAME, credentials.username)
            putString(KEY_PASSWORD_ENCRYPTED, base64EncryptedPassword)
            apply()
        }

        _credentialsState.value = credentials
    }

    /**
     * Clears credentials from storage, effectively logging the user out.
     */
    fun clearCredentials() {
        prefs.edit().apply {
            clear()
            apply()
        }
        _credentialsState.value = null
    }

    /**
     * Internal method to read and decrypt credentials from storage on startup.
     *
     * @return The decrypted [SubsonicCredentials] or null if missing or decryption fails.
     */
    private fun loadCredentials(): SubsonicCredentials? {
        val serverUrl = prefs.getString(KEY_SERVER_URL, null)
        val username = prefs.getString(KEY_USERNAME, null)
        val encryptedPasswordBase64 = prefs.getString(KEY_PASSWORD_ENCRYPTED, null)

        if (serverUrl.isNullOrBlank() || username.isNullOrBlank() || encryptedPasswordBase64.isNullOrBlank()) {
            return null
        }

        return try {
            val encryptedBytes = Base64.decode(encryptedPasswordBase64, Base64.NO_WRAP)
            val decryptedPassword = cryptoManager.decrypt(encryptedBytes)
            SubsonicCredentials(
                serverUrl = serverUrl,
                username = username,
                passwordPlain = decryptedPassword
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            null
        }
    }
}
