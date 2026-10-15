package com.vsulimov.resonance.domain.repository

import com.vsulimov.libsubsonic.SubsonicClient
import com.vsulimov.libsubsonic.data.response.browsing.Album
import com.vsulimov.libsubsonic.data.response.browsing.Artist
import com.vsulimov.libsubsonic.data.response.browsing.Child
import com.vsulimov.libsubsonic.data.response.playlists.Playlist
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface providing offline data queries from Room database.
 */
interface OfflineRepository {

    /**
     * Observes all offline albums sorted by name.
     */
    fun observeAllAlbums(): Flow<List<Album>>

    /**
     * Searches offline albums matching a query.
     */
    fun searchAlbums(query: String): Flow<List<Album>>

    /**
     * Observes an offline album with its contained songs by album ID.
     */
    fun observeAlbumDetails(albumId: String): Flow<Album?>

    /**
     * Observes all offline artists sorted by name.
     */
    fun observeAllArtists(): Flow<List<Artist>>

    /**
     * Searches offline artists matching a query.
     */
    fun searchArtists(query: String): Flow<List<Artist>>

    /**
     * Observes an offline artist with associated offline albums by artist ID.
     */
    fun observeArtistDetails(artistId: String): Flow<Artist?>

    /**
     * Observes all offline playlists sorted by name.
     */
    fun observeAllPlaylists(): Flow<List<Playlist>>

    /**
     * Searches offline playlists matching a query.
     */
    fun searchPlaylists(query: String): Flow<List<Playlist>>

    /**
     * Observes an offline playlist with its contained songs by playlist ID.
     */
    fun observePlaylistDetails(playlistId: String): Flow<Playlist?>

    /**
     * Observes ordered song IDs contained within a playlist by playlist ID.
     *
     * @param playlistId Target playlist ID.
     * @return [Flow] emitting ordered song IDs for playlist.
     */
    fun observeSongIdsForPlaylist(playlistId: String): Flow<List<String>>

    /**
     * Performs incremental background synchronization of playlist metadata and cross-references.
     *
     * @param getClient Provider function returning an authenticated [SubsonicClient].
     */
    suspend fun syncPlaylistsGraph(getClient: () -> SubsonicClient?)

    /**
     * Observes all offline starred/favorited songs.
     */
    fun observeStarredSongs(): Flow<List<Child>>

    /**
     * Observes all offline starred/favorited albums.
     */
    fun observeStarredAlbums(): Flow<List<Album>>

    /**
     * Observes all offline starred/favorited artists.
     */
    fun observeStarredArtists(): Flow<List<Artist>>

    /**
     * Searches offline songs matching a query.
     */
    fun searchSongs(query: String): Flow<List<Child>>
}
