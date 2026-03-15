package com.vsulimov.resonance.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.vsulimov.resonance.domain.model.ServerCredentials
import com.vsulimov.resonance.domain.repository.CredentialsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * [CredentialsRepository] implementation using [SharedPreferences].
 *
 * Credentials are stored in a private preferences file accessible only
 * to this application. Android's file-based encryption (FBE) protects
 * app-private data at rest when the device is locked.
 *
 * All disk I/O is dispatched to [Dispatchers.IO].
 *
 * @param context Application context used to create the preferences file.
 */
class CredentialsRepositoryImpl(context: Context) : CredentialsRepository {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)

    override suspend fun save(credentials: ServerCredentials): Unit = withContext(Dispatchers.IO) {
        prefs.edit {
            putString(KEY_SERVER_URL, credentials.serverUrl)
                .putString(KEY_USERNAME, credentials.username)
                .putString(KEY_PASSWORD, credentials.password)
        }
    }

    override suspend fun load(): ServerCredentials? = withContext(Dispatchers.IO) {
        val url = prefs.getString(KEY_SERVER_URL, null) ?: return@withContext null
        val username = prefs.getString(KEY_USERNAME, null) ?: return@withContext null
        val password = prefs.getString(KEY_PASSWORD, null) ?: return@withContext null
        ServerCredentials(serverUrl = url, username = username, password = password)
    }

    override suspend fun clear(): Unit = withContext(Dispatchers.IO) {
        prefs.edit { clear() }
    }

    private companion object {
        const val PREFS_FILE_NAME = "resonance_credentials"
        const val KEY_SERVER_URL = "server_url"
        const val KEY_USERNAME = "username"
        const val KEY_PASSWORD = "password"
    }
}
