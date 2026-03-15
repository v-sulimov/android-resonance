package com.vsulimov.resonance.domain.repository

import com.vsulimov.resonance.domain.model.SearchResult

/**
 * Abstraction for searching the music library on the Subsonic server.
 *
 * Wraps the Subsonic `search3` API, returning grouped results across
 * artists, albums, and songs.
 */
interface SearchRepository {

    /**
     * Searches the library for the given [query].
     *
     * @param query Search query text.
     * @param artistCount Maximum number of artist results to return.
     * @param albumCount Maximum number of album results to return.
     * @param songCount Maximum number of song results to return.
     * @return A [Result] containing grouped [SearchResult] on success,
     *   or a failure with the underlying error.
     */
    suspend fun search(
        query: String,
        artistCount: Int,
        albumCount: Int,
        songCount: Int
    ): Result<SearchResult>
}
