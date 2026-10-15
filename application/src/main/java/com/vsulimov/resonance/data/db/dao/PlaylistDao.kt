package com.vsulimov.resonance.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.vsulimov.resonance.data.db.entity.OfflinePlaylistEntity
import com.vsulimov.resonance.data.db.entity.OfflineSongEntity
import com.vsulimov.resonance.data.db.entity.PlaylistSongCrossRefEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for managing offline playlists and cross-references in Room database.
 */
@Dao
interface PlaylistDao {

    /**
     * Fetches a playlist entity by unique ID.
     *
     * @param id Target playlist ID.
     * @return [OfflinePlaylistEntity] or null if not found.
     */
    @Query("SELECT * FROM offline_playlists WHERE id = :id")
    suspend fun getPlaylistById(id: String): OfflinePlaylistEntity?

    /**
     * Observes a playlist entity by ID.
     *
     * @param id Target playlist ID.
     * @return [Flow] emitting playlist updates or null.
     */
    @Query("SELECT * FROM offline_playlists WHERE id = :id")
    fun observePlaylistById(id: String): Flow<OfflinePlaylistEntity?>

    /**
     * Observes all offline playlists sorted alphabetically by name.
     *
     * @return [Flow] emitting list of all playlists.
     */
    @Query("SELECT * FROM offline_playlists ORDER BY name ASC")
    fun observeAllPlaylists(): Flow<List<OfflinePlaylistEntity>>

    /**
     * Searches offline playlists matching query against name.
     *
     * @param query Search string.
     * @return [Flow] emitting matching playlists.
     */
    @Query("SELECT * FROM offline_playlists WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchPlaylists(query: String): Flow<List<OfflinePlaylistEntity>>

    /**
     * Observes songs contained within a playlist ordered by cross-reference position.
     *
     * @param playlistId Target playlist ID.
     * @return [Flow] emitting ordered song entities for playlist.
     */
    @Query(
        """
        SELECT s.* FROM offline_songs s
        INNER JOIN playlist_song_cross_ref ref ON s.id = ref.songId
        WHERE ref.playlistId = :playlistId
        ORDER BY ref.position ASC
        """
    )
    fun observeSongsForPlaylist(playlistId: String): Flow<List<OfflineSongEntity>>

    /**
     * Retrieves songs contained within a playlist ordered by cross-reference position.
     *
     * @param playlistId Target playlist ID.
     * @return Ordered song entities for playlist.
     */
    @Query(
        """
        SELECT s.* FROM offline_songs s
        INNER JOIN playlist_song_cross_ref ref ON s.id = ref.songId
        WHERE ref.playlistId = :playlistId
        ORDER BY ref.position ASC
        """
    )
    suspend fun getSongsForPlaylist(playlistId: String): List<OfflineSongEntity>

    /**
     * Upserts a playlist entity.
     *
     * @param playlist Playlist entity to save.
     */
    @Upsert
    suspend fun insertPlaylist(playlist: OfflinePlaylistEntity)

    /**
     * Upserts a list of playlist entities.
     *
     * @param playlists List of playlist entities to save.
     */
    @Upsert
    suspend fun insertPlaylists(playlists: List<OfflinePlaylistEntity>)

    /**
     * Inserts or replaces cross-reference entities.
     *
     * @param crossRefs List of playlist-song mappings.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistSongCrossRefs(crossRefs: List<PlaylistSongCrossRefEntity>)

    /**
     * Deletes all cross-reference entries for a playlist.
     *
     * @param playlistId Target playlist ID.
     */
    @Query("DELETE FROM playlist_song_cross_ref WHERE playlistId = :playlistId")
    suspend fun deletePlaylistSongCrossRefs(playlistId: String)

    /**
     * Observes ordered song IDs contained within a playlist by cross-reference position.
     *
     * @param playlistId Target playlist ID.
     * @return [Flow] emitting ordered song ID list for playlist.
     */
    @Query("SELECT songId FROM playlist_song_cross_ref WHERE playlistId = :playlistId ORDER BY position ASC")
    fun observeSongIdsForPlaylist(playlistId: String): Flow<List<String>>

    /**
     * Retrieves ordered song IDs contained within a playlist by cross-reference position.
     *
     * @param playlistId Target playlist ID.
     * @return Ordered song ID list for playlist.
     */
    @Query("SELECT songId FROM playlist_song_cross_ref WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun getSongIdsForPlaylist(playlistId: String): List<String>

    /**
     * Counts how many cross-reference entries exist for a playlist.
     *
     * @param playlistId Target playlist ID.
     * @return Number of cross-reference entries.
     */
    @Query("SELECT COUNT(*) FROM playlist_song_cross_ref WHERE playlistId = :playlistId")
    suspend fun getPlaylistSongCount(playlistId: String): Int

    /**
     * Transactionally updates playlist and replaces all contained song cross-references in order.
     *
     * @param playlist Playlist entity.
     * @param songs Ordered list of song entities within playlist.
     */
    @Transaction
    suspend fun updatePlaylistWithSongs(playlist: OfflinePlaylistEntity, songs: List<OfflineSongEntity>) {
        insertPlaylist(playlist)
        deletePlaylistSongCrossRefs(playlist.id)
        val crossRefs = songs.mapIndexed { index, song ->
            PlaylistSongCrossRefEntity(
                playlistId = playlist.id,
                songId = song.id,
                position = index
            )
        }
        insertPlaylistSongCrossRefs(crossRefs)
    }

    /**
     * Transactionally updates playlist entity and replaces all contained song ID cross-references in order.
     *
     * @param playlist Playlist entity.
     * @param songIds Ordered list of song IDs within playlist.
     */
    @Transaction
    suspend fun updatePlaylistWithSongIds(playlist: OfflinePlaylistEntity, songIds: List<String>) {
        insertPlaylist(playlist)
        deletePlaylistSongCrossRefs(playlist.id)
        val crossRefs = songIds.mapIndexed { index, songId ->
            PlaylistSongCrossRefEntity(
                playlistId = playlist.id,
                songId = songId,
                position = index
            )
        }
        insertPlaylistSongCrossRefs(crossRefs)
    }

    /**
     * Updates an existing playlist entity.
     *
     * @param playlist Playlist entity with updated values.
     */
    @Update
    suspend fun updatePlaylist(playlist: OfflinePlaylistEntity)

    /**
     * Deletes a playlist entity.
     *
     * @param playlist Playlist entity to delete.
     */
    @Delete
    suspend fun deletePlaylist(playlist: OfflinePlaylistEntity)

    /**
     * Deletes a playlist entity by ID.
     *
     * @param id Target playlist ID.
     */
    @Query("DELETE FROM offline_playlists WHERE id = :id")
    suspend fun deletePlaylistById(id: String)

    /**
     * Clears all playlists from database.
     */
    @Query("DELETE FROM offline_playlists")
    suspend fun clearAll()

    /**
     * Counts how many playlists use a specific cover art ID.
     *
     * @param coverArtId Target cover art ID.
     * @return Number of playlists using the cover art.
     */
    @Query("SELECT COUNT(*) FROM offline_playlists WHERE coverArt = :coverArtId")
    suspend fun countPlaylistsWithCoverArt(coverArtId: String): Int
}
