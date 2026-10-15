package com.vsulimov.resonance.domain.repository

import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for managing, caching, and toggling favorite (starred) status
 * for songs, albums, and artists across the application.
 */
interface FavoritesRepository {

    /** Observable flow of starred song IDs. */
    val starredSongIds: StateFlow<Set<String>>

    /** Observable flow of starred album IDs. */
    val starredAlbumIds: StateFlow<Set<String>>

    /** Observable flow of starred artist IDs. */
    val starredArtistIds: StateFlow<Set<String>>

    /**
     * Loads the latest starred items list from the Subsonic server.
     *
     * @return Result indicating success or failure.
     */
    suspend fun loadFavorites(): Result<Unit>

    /**
     * Toggles the favorite status for a song with [songId].
     *
     * @param songId Unique song ID.
     * @return Result indicating success (with the new starred status) or failure.
     */
    suspend fun toggleSongFavorite(songId: String): Result<Boolean>

    /**
     * Toggles the favorite status for an album with [albumId].
     *
     * @param albumId Unique album ID.
     * @return Result indicating success (with the new starred status) or failure.
     */
    suspend fun toggleAlbumFavorite(albumId: String): Result<Boolean>

    /**
     * Toggles the favorite status for an artist with [artistId].
     *
     * @param artistId Unique artist ID.
     * @return Result indicating success (with the new starred status) or failure.
     */
    suspend fun toggleArtistFavorite(artistId: String): Result<Boolean>

    /**
     * Checks if a song is currently starred.
     *
     * @param songId Unique song ID.
     * @return True if starred, false otherwise.
     */
    fun isSongStarred(songId: String): Boolean

    /**
     * Checks if an album is currently starred.
     *
     * @param albumId Unique album ID.
     * @return True if starred, false otherwise.
     */
    fun isAlbumStarred(albumId: String): Boolean

    /**
     * Checks if an artist is currently starred.
     *
     * @param artistId Unique artist ID.
     * @return True if starred, false otherwise.
     */
    fun isArtistStarred(artistId: String): Boolean

    /**
     * Registers a song as starred in local memory cache.
     *
     * @param songId Unique song ID.
     */
    fun registerStarredSong(songId: String)

    /**
     * Registers an album as starred in local memory cache.
     *
     * @param albumId Unique album ID.
     */
    fun registerStarredAlbum(albumId: String)

    /**
     * Registers an artist as starred in local memory cache.
     *
     * @param artistId Unique artist ID.
     */
    fun registerStarredArtist(artistId: String)

    /**
     * Clears all cached in-memory starred item IDs.
     */
    fun clearFavorites()
}
