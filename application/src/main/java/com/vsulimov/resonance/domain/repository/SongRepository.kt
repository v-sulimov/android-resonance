package com.vsulimov.resonance.domain.repository

import com.vsulimov.resonance.domain.model.SongSummary

/**
 * Repository for retrieving songs from the music library.
 */
interface SongRepository {

    /**
     * Fetches a paginated list of songs from the library.
     *
     * @param size Maximum number of songs to return.
     * @param offset Zero-based index of the first song to return.
     * @return [Result.success] with the song list, or [Result.failure] on error.
     */
    suspend fun getSongs(size: Int, offset: Int): Result<List<SongSummary>>
}
