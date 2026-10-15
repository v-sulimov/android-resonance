package com.vsulimov.resonance.data.repository

import com.vsulimov.libsubsonic.SubsonicClient
import com.vsulimov.libsubsonic.data.response.browsing.Album
import com.vsulimov.libsubsonic.data.response.browsing.Artist
import com.vsulimov.libsubsonic.data.response.browsing.Child
import com.vsulimov.libsubsonic.data.response.playlists.Playlist
import com.vsulimov.libsubsonic.data.result.SubsonicResult
import com.vsulimov.resonance.data.db.ResonanceDatabase
import com.vsulimov.resonance.data.db.mapper.toAlbum
import com.vsulimov.resonance.data.db.mapper.toArtist
import com.vsulimov.resonance.data.db.mapper.toChild
import com.vsulimov.resonance.data.db.mapper.toOfflineEntity
import com.vsulimov.resonance.data.db.mapper.toPlaylist
import com.vsulimov.resonance.domain.repository.OfflineRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Default implementation of [OfflineRepository] querying Room database entities and mapping them to domain models.
 */
class DefaultOfflineRepository(private val database: ResonanceDatabase) : OfflineRepository {

    override fun observeAllAlbums(): Flow<List<Album>> = database.albumDao().observeAllAlbums().map { entities ->
        entities.map { it.toAlbum() }
    }

    override fun searchAlbums(query: String): Flow<List<Album>> =
        database.albumDao().searchAlbums(query).map { entities ->
            entities.map { it.toAlbum() }
        }

    override fun observeAlbumDetails(albumId: String): Flow<Album?> {
        val albumFlow = database.albumDao().observeAlbumById(albumId)
        val songsFlow = database.songDao().observeSongsByAlbumId(albumId)
        return combine(albumFlow, songsFlow) { albumEntity, songEntities ->
            albumEntity?.toAlbum(songs = songEntities.map { it.toChild() })
        }
    }

    override fun observeAllArtists(): Flow<List<Artist>> = database.artistDao().observeAllArtists().map { entities ->
        entities.map { it.toArtist() }
    }

    override fun searchArtists(query: String): Flow<List<Artist>> =
        database.artistDao().searchArtists(query).map { entities ->
            entities.map { it.toArtist() }
        }

    override fun observeArtistDetails(artistId: String): Flow<Artist?> {
        val artistFlow = database.artistDao().observeArtistById(artistId)
        val albumsFlow = database.albumDao().observeAlbumsByArtistId(artistId)
        return combine(artistFlow, albumsFlow) { artistEntity, albumEntities ->
            artistEntity?.toArtist(albums = albumEntities.map { it.toAlbum() })
        }
    }

    override fun observeAllPlaylists(): Flow<List<Playlist>> =
        database.playlistDao().observeAllPlaylists().map { entities ->
            entities.map { it.toPlaylist() }
        }

    override fun searchPlaylists(query: String): Flow<List<Playlist>> =
        database.playlistDao().searchPlaylists(query).map { entities ->
            entities.map { it.toPlaylist() }
        }

    override fun observePlaylistDetails(playlistId: String): Flow<Playlist?> {
        val playlistFlow = database.playlistDao().observePlaylistById(playlistId)
        val songsFlow = database.playlistDao().observeSongsForPlaylist(playlistId)
        return combine(playlistFlow, songsFlow) { playlistEntity, songEntities ->
            playlistEntity?.toPlaylist(entries = songEntities.map { it.toChild() })
        }
    }

    override fun observeSongIdsForPlaylist(playlistId: String): Flow<List<String>> =
        database.playlistDao().observeSongIdsForPlaylist(playlistId)

    override suspend fun syncPlaylistsGraph(getClient: () -> SubsonicClient?) {
        withContext(Dispatchers.IO) {
            val client = getClient() ?: return@withContext
            val result = client.getPlaylists()
            if (result !is SubsonicResult.Success) return@withContext

            val playlists = result.data.playlists
            playlists.forEach { serverPlaylist ->
                val localEntity = database.playlistDao().getPlaylistById(serverPlaylist.id)
                val localCrossRefCount = database.playlistDao().getPlaylistSongCount(serverPlaylist.id)

                val needsEntriesSync = (localEntity == null) ||
                    (localEntity.songCount != serverPlaylist.songCount) ||
                    (localCrossRefCount != serverPlaylist.songCount)

                if (needsEntriesSync) {
                    val detailResult = client.getPlaylist(serverPlaylist.id)
                    if (detailResult is SubsonicResult.Success) {
                        val fullPlaylist = detailResult.data.playlist
                        val songEntities = fullPlaylist.entries.map { child ->
                            val existing = database.songDao().getSongById(child.id)
                            child.toOfflineEntity(
                                localFilePath = existing?.localFilePath,
                                downloadedAt = existing?.downloadedAt ?: System.currentTimeMillis()
                            )
                        }
                        database.songDao().insertSongs(songEntities)
                        database.playlistDao().updatePlaylistWithSongs(
                            playlist = fullPlaylist.toOfflineEntity(),
                            songs = songEntities
                        )
                    } else {
                        database.playlistDao().insertPlaylist(serverPlaylist.toOfflineEntity())
                    }
                } else {
                    database.playlistDao().insertPlaylist(serverPlaylist.toOfflineEntity())
                }
            }
        }
    }

    override fun observeStarredSongs(): Flow<List<Child>> = database.songDao().observeStarredSongs().map { entities ->
        entities.map { it.toChild() }
    }

    override fun observeStarredAlbums(): Flow<List<Album>> =
        database.albumDao().observeStarredAlbums().map { entities ->
            entities.map { it.toAlbum() }
        }

    override fun observeStarredArtists(): Flow<List<Artist>> =
        database.artistDao().observeStarredArtists().map { entities ->
            entities.map { it.toArtist() }
        }

    override fun searchSongs(query: String): Flow<List<Child>> = database.songDao().searchSongs(query).map { entities ->
        entities.map { it.toChild() }
    }
}
