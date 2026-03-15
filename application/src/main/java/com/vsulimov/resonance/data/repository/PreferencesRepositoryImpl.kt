package com.vsulimov.resonance.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.vsulimov.resonance.domain.model.AudioQuality
import com.vsulimov.resonance.domain.model.CacheLimit
import com.vsulimov.resonance.domain.model.ThemePreference
import com.vsulimov.resonance.domain.repository.PreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * [PreferencesRepository] implementation using [SharedPreferences].
 *
 * Preferences are stored in a private preferences file accessible only
 * to this application. The [themePreference] flow is initialized
 * synchronously from the stored value (a fast in-memory read after the
 * first [SharedPreferences] access) so the correct theme is applied
 * before the first frame renders.
 *
 * All disk I/O is dispatched to [Dispatchers.IO].
 *
 * @param context Application context used to create the preferences file.
 */
class PreferencesRepositoryImpl(context: Context) : PreferencesRepository {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)

    private val _themePreference = MutableStateFlow(readThemePreference())

    override val themePreference: StateFlow<ThemePreference> = _themePreference.asStateFlow()

    override suspend fun setThemePreference(preference: ThemePreference) {
        _themePreference.value = preference
        withContext(Dispatchers.IO) {
            prefs.edit { putString(KEY_THEME, preference.name) }
        }
    }

    override suspend fun getAudioQuality(): AudioQuality = withContext(Dispatchers.IO) {
        val name = prefs.getString(KEY_AUDIO_QUALITY, null)
        name?.let { enumValueOrNull<AudioQuality>(it) } ?: AudioQuality.ORIGINAL
    }

    override suspend fun setAudioQuality(quality: AudioQuality): Unit = withContext(Dispatchers.IO) {
        prefs.edit { putString(KEY_AUDIO_QUALITY, quality.name) }
    }

    override suspend fun getCacheLimit(): CacheLimit = withContext(Dispatchers.IO) {
        val name = prefs.getString(KEY_CACHE_LIMIT, null)
        name?.let { enumValueOrNull<CacheLimit>(it) } ?: CacheLimit.GB_1
    }

    override suspend fun setCacheLimit(limit: CacheLimit): Unit = withContext(Dispatchers.IO) {
        prefs.edit { putString(KEY_CACHE_LIMIT, limit.name) }
    }

    override suspend fun getServerVersion(): String? = withContext(Dispatchers.IO) {
        prefs.getString(KEY_SERVER_VERSION, null)
    }

    override suspend fun setServerVersion(version: String): Unit = withContext(Dispatchers.IO) {
        prefs.edit { putString(KEY_SERVER_VERSION, version) }
    }

    override suspend fun getServerType(): String? = withContext(Dispatchers.IO) {
        prefs.getString(KEY_SERVER_TYPE, null)
    }

    override suspend fun setServerType(type: String): Unit = withContext(Dispatchers.IO) {
        prefs.edit { putString(KEY_SERVER_TYPE, type) }
    }

    override suspend fun getSearchHistory(): List<String> = withContext(Dispatchers.IO) {
        val raw = prefs.getString(KEY_SEARCH_HISTORY, null)
        if (raw.isNullOrEmpty()) emptyList() else raw.split(HISTORY_SEPARATOR)
    }

    override suspend fun addSearchHistoryEntry(query: String): Unit = withContext(Dispatchers.IO) {
        val current = getSearchHistoryInternal().toMutableList()
        current.remove(query)
        current.add(0, query)
        if (current.size > MAX_HISTORY_SIZE) {
            current.subList(MAX_HISTORY_SIZE, current.size).clear()
        }
        prefs.edit { putString(KEY_SEARCH_HISTORY, current.joinToString(HISTORY_SEPARATOR)) }
    }

    override suspend fun removeSearchHistoryEntry(query: String): Unit = withContext(Dispatchers.IO) {
        val current = getSearchHistoryInternal().toMutableList()
        current.remove(query)
        prefs.edit { putString(KEY_SEARCH_HISTORY, current.joinToString(HISTORY_SEPARATOR)) }
    }

    override suspend fun clearSearchHistory(): Unit = withContext(Dispatchers.IO) {
        prefs.edit { remove(KEY_SEARCH_HISTORY) }
    }

    override suspend fun clear(): Unit = withContext(Dispatchers.IO) {
        prefs.edit { clear() }
        _themePreference.value = ThemePreference.SYSTEM
    }

    /**
     * Reads the theme preference synchronously from [SharedPreferences].
     *
     * Called once during construction so the initial [StateFlow] value
     * is available before any composable collects it.
     */
    private fun readThemePreference(): ThemePreference {
        val name = prefs.getString(KEY_THEME, null)
        return name?.let { enumValueOrNull<ThemePreference>(it) } ?: ThemePreference.SYSTEM
    }

    /**
     * Internal synchronous read of search history, for use within
     * [Dispatchers.IO] blocks that already handle threading.
     */
    private fun getSearchHistoryInternal(): List<String> {
        val raw = prefs.getString(KEY_SEARCH_HISTORY, null)
        return if (raw.isNullOrEmpty()) emptyList() else raw.split(HISTORY_SEPARATOR)
    }

    private companion object {
        const val PREFS_FILE_NAME = "resonance_preferences"
        const val KEY_THEME = "theme"
        const val KEY_AUDIO_QUALITY = "audio_quality"
        const val KEY_CACHE_LIMIT = "cache_limit"
        const val KEY_SERVER_VERSION = "server_version"
        const val KEY_SERVER_TYPE = "server_type"
        const val KEY_SEARCH_HISTORY = "search_history"
        const val HISTORY_SEPARATOR = "\n"
        const val MAX_HISTORY_SIZE = 10
    }
}

/**
 * Safely converts a string to an enum value, returning `null` if the
 * string does not match any entry. Avoids [IllegalArgumentException]
 * when stored values become stale after an app update.
 */
private inline fun <reified T : Enum<T>> enumValueOrNull(name: String): T? =
    try {
        enumValueOf<T>(name)
    } catch (_: IllegalArgumentException) {
        null
    }
