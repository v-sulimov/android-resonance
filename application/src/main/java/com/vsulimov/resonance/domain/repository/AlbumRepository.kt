package com.vsulimov.resonance.domain.repository

import com.vsulimov.resonance.domain.model.AlbumDetail
import com.vsulimov.resonance.domain.model.AlbumSortType
import com.vsulimov.resonance.domain.model.AlbumSummary

/**
 * Abstraction for retrieving album data from the music server.
 *
 * Provides both list operations for carousels and detail retrieval
 * for the album detail screen. All methods return [Result] to allow
 * callers to handle failures gracefully.
 */
interface AlbumRepository {

    /**
     * Returns full album details including the track list.
     *
     * @param id Unique album identifier.
     * @return [Result.success] with an [AlbumDetail], or
     *   [Result.failure] wrapping the server error.
     */
    suspend fun getAlbumDetail(id: String): Result<AlbumDetail>

    /**
     * Returns albums that the user has played most recently.
     *
     * @param count Maximum number of albums to return.
     * @return [Result.success] with a list of [AlbumSummary], or
     *   [Result.failure] wrapping the server error.
     */
    suspend fun getRecentlyPlayed(count: Int): Result<List<AlbumSummary>>

    /**
     * Returns the most frequently played albums.
     *
     * @param count Maximum number of albums to return.
     * @return [Result.success] with a list of [AlbumSummary], or
     *   [Result.failure] wrapping the server error.
     */
    suspend fun getMostPlayed(count: Int): Result<List<AlbumSummary>>

    /**
     * Returns the most recently added albums.
     *
     * @param count Maximum number of albums to return.
     * @return [Result.success] with a list of [AlbumSummary], or
     *   [Result.failure] wrapping the server error.
     */
    suspend fun getRecentlyAdded(count: Int): Result<List<AlbumSummary>>

    /**
     * Returns a random selection of albums.
     *
     * @param count Maximum number of albums to return.
     * @return [Result.success] with a list of [AlbumSummary], or
     *   [Result.failure] wrapping the server error.
     */
    suspend fun getRandomPicks(count: Int): Result<List<AlbumSummary>>

    /**
     * Returns a paginated list of albums with the given sort/filter type.
     *
     * @param sortType Determines the ordering or filtering applied by the server.
     * @param size Maximum number of albums to return per page.
     * @param offset Zero-based offset into the album list for pagination.
     * @return [Result.success] with a list of [AlbumSummary], or
     *   [Result.failure] wrapping the server error.
     */
    suspend fun getAlbums(sortType: AlbumSortType, size: Int, offset: Int): Result<List<AlbumSummary>>
}
