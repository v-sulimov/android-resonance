package com.vsulimov.resonance.domain.repository

/**
 * Abstraction for retrieving library-wide category counts from the music server.
 *
 * Related counts that share an API call are grouped into a single method
 * to avoid redundant network requests. All methods return [Result] to
 * allow callers to handle failures gracefully.
 */
interface LibraryRepository {

    /**
     * Returns the total number of artists in the library.
     *
     * Derived from the artist index endpoint, which returns all artists
     * grouped alphabetically.
     *
     * @return [Result.success] with the artist count, or
     *   [Result.failure] wrapping the server error.
     */
    suspend fun getArtistCount(): Result<Int>

    /**
     * Returns the total number of albums and songs in the library.
     *
     * Both counts are derived by paginating through the full album list.
     * The album count is the total number of albums returned, and the
     * song count is the sum of each album's [songCount] field.
     *
     * @return [Result.success] with a [Pair] of (albumCount, songCount),
     *   or [Result.failure] wrapping the server error.
     */
    suspend fun getAlbumAndSongCounts(): Result<Pair<Int, Int>>

    /**
     * Returns the total number of distinct genres in the library.
     *
     * @return [Result.success] with the genre count, or
     *   [Result.failure] wrapping the server error.
     */
    suspend fun getGenreCount(): Result<Int>

    /**
     * Returns the total number of user playlists.
     *
     * @return [Result.success] with the playlist count, or
     *   [Result.failure] wrapping the server error.
     */
    suspend fun getPlaylistCount(): Result<Int>

    /**
     * Returns the total number of starred (favorited) tracks.
     *
     * @return [Result.success] with the favorite track count, or
     *   [Result.failure] wrapping the server error.
     */
    suspend fun getFavoriteTrackCount(): Result<Int>
}
