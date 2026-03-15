package com.vsulimov.resonance.domain.repository

import com.vsulimov.resonance.domain.model.AudioQuality
import com.vsulimov.resonance.domain.model.CacheLimit
import com.vsulimov.resonance.domain.model.ThemePreference
import kotlinx.coroutines.flow.StateFlow

/**
 * Abstraction for persisting and retrieving user preferences.
 *
 * Preferences include theme selection, audio quality, cache limits,
 * and server metadata that supplements the core credentials.
 */
interface PreferencesRepository {

    /**
     * Reactive stream of the current theme preference.
     *
     * Updated synchronously when [setThemePreference] is called,
     * and initialized from persistent storage on construction.
     * Observed by the theme composable to apply changes immediately.
     */
    val themePreference: StateFlow<ThemePreference>

    /** Persists the given [preference] and updates [themePreference]. */
    suspend fun setThemePreference(preference: ThemePreference)

    /** Returns the stored audio quality preference. */
    suspend fun getAudioQuality(): AudioQuality

    /** Persists the given audio [quality] preference. */
    suspend fun setAudioQuality(quality: AudioQuality)

    /** Returns the stored cache limit preference. */
    suspend fun getCacheLimit(): CacheLimit

    /** Persists the given cache [limit] preference. */
    suspend fun setCacheLimit(limit: CacheLimit)

    /**
     * Returns the stored server version string (e.g. "0.53.3"),
     * or `null` if no server info has been saved.
     */
    suspend fun getServerVersion(): String?

    /** Persists the server [version] string received during connection. */
    suspend fun setServerVersion(version: String)

    /**
     * Returns the stored server type identifier (e.g. "navidrome"),
     * or `null` if no server info has been saved.
     */
    suspend fun getServerType(): String?

    /** Persists the server [type] identifier received during connection. */
    suspend fun setServerType(type: String)

    /** Returns the search history list, most recent first. */
    suspend fun getSearchHistory(): List<String>

    /** Adds a query to the search history (moves to front if already present). */
    suspend fun addSearchHistoryEntry(query: String)

    /** Removes a single entry from the search history. */
    suspend fun removeSearchHistoryEntry(query: String)

    /** Clears all search history entries. */
    suspend fun clearSearchHistory()

    /** Removes all stored preferences. Called during logout. */
    suspend fun clear()
}
