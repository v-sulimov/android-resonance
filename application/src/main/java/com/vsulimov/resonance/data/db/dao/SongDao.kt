package com.vsulimov.resonance.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vsulimov.resonance.data.db.entity.OfflineSongEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for managing offline songs in Room database.
 */
@Dao
interface SongDao {

    /**
     * Fetches a song entity by its unique ID.
     *
     * @param id Target song ID.
     * @return [OfflineSongEntity] or null if not found.
     */
    @Query("SELECT * FROM offline_songs WHERE id = :id")
    suspend fun getSongById(id: String): OfflineSongEntity?

    /**
     * Observes a single song entity by ID.
     *
     * @param id Target song ID.
     * @return [Flow] emitting song updates or null.
     */
    @Query("SELECT * FROM offline_songs WHERE id = :id")
    fun observeSongById(id: String): Flow<OfflineSongEntity?>

    /**
     * Observes all downloaded song entities sorted alphabetically by title.
     *
     * @return [Flow] emitting list of all offline songs.
     */
    @Query("SELECT * FROM offline_songs ORDER BY title ASC")
    fun observeAllSongs(): Flow<List<OfflineSongEntity>>

    /**
     * Observes songs belonging to a specific album sorted by disc, track, and title.
     *
     * @param albumId Target album ID.
     * @return [Flow] emitting list of songs for specified album.
     */
    @Query("SELECT * FROM offline_songs WHERE albumId = :albumId ORDER BY discNumber ASC, track ASC, title ASC")
    fun observeSongsByAlbumId(albumId: String): Flow<List<OfflineSongEntity>>

    /**
     * Retrieves songs belonging to a specific album sorted by disc, track, and title.
     *
     * @param albumId Target album ID.
     * @return List of songs for specified album.
     */
    @Query("SELECT * FROM offline_songs WHERE albumId = :albumId ORDER BY discNumber ASC, track ASC, title ASC")
    suspend fun getSongsByAlbumId(albumId: String): List<OfflineSongEntity>

    /**
     * Retrieves songs matching album ID or album title string.
     *
     * @param albumId Target album ID.
     * @param albumName Target album title.
     * @return List of matching song entities.
     */
    @Query(
        "SELECT * FROM offline_songs WHERE albumId = :albumId OR album = :albumName ORDER BY discNumber ASC, track ASC, title ASC"
    )
    suspend fun getSongsByAlbumIdOrName(albumId: String, albumName: String): List<OfflineSongEntity>

    /**
     * Observes songs associated with a specific artist sorted by title.
     *
     * @param artistId Target artist ID.
     * @return [Flow] emitting list of songs for specified artist.
     */
    @Query("SELECT * FROM offline_songs WHERE artistId = :artistId ORDER BY title ASC")
    fun observeSongsByArtistId(artistId: String): Flow<List<OfflineSongEntity>>

    /**
     * Retrieves songs associated with a specific artist sorted by title.
     *
     * @param artistId Target artist ID.
     * @return List of songs for specified artist.
     */
    @Query("SELECT * FROM offline_songs WHERE artistId = :artistId ORDER BY title ASC")
    suspend fun getSongsByArtistId(artistId: String): List<OfflineSongEntity>

    /**
     * Observes all starred/favorited offline songs.
     *
     * @return [Flow] emitting list of starred songs.
     */
    @Query("SELECT * FROM offline_songs WHERE isStarred = 1 ORDER BY title ASC")
    fun observeStarredSongs(): Flow<List<OfflineSongEntity>>

    /**
     * Searches offline songs matching query against title, artist, or album.
     *
     * @param query Search string.
     * @return [Flow] emitting matching songs.
     */
    @Query(
        "SELECT * FROM offline_songs WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR album LIKE '%' || :query || '%' ORDER BY title ASC"
    )
    fun searchSongs(query: String): Flow<List<OfflineSongEntity>>

    /**
     * Retrieves list of all downloaded song IDs.
     *
     * @return List of song ID strings.
     */
    @Query("SELECT id FROM offline_songs")
    suspend fun getAllDownloadedSongIds(): List<String>

    /**
     * Inserts or replaces a song entity.
     *
     * @param song Song entity to save.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: OfflineSongEntity)

    /**
     * Inserts or replaces a list of song entities.
     *
     * @param songs List of song entities to save.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<OfflineSongEntity>)

    /**
     * Updates an existing song entity.
     *
     * @param song Song entity with updated values.
     */
    @Update
    suspend fun updateSong(song: OfflineSongEntity)

    /**
     * Updates starred status of a target song.
     *
     * @param id Target song ID.
     * @param isStarred New starred state.
     */
    @Query("UPDATE offline_songs SET isStarred = :isStarred WHERE id = :id")
    suspend fun updateSongStarred(id: String, isStarred: Boolean)

    /**
     * Deletes a song entity.
     *
     * @param song Song entity to delete.
     */
    @Delete
    suspend fun deleteSong(song: OfflineSongEntity)

    /**
     * Deletes a song entity by ID.
     *
     * @param id Target song ID.
     */
    @Query("DELETE FROM offline_songs WHERE id = :id")
    suspend fun deleteSongById(id: String)

    /**
     * Clears all songs from database.
     */
    @Query("DELETE FROM offline_songs")
    suspend fun clearAll()

    /**
     * Counts how many playlists contain a specific song.
     *
     * @param songId Target song ID.
     * @return Number of playlists containing the song.
     */
    @Query("SELECT COUNT(*) FROM playlist_song_cross_ref WHERE songId = :songId")
    suspend fun getPlaylistCountForSong(songId: String): Int

    /**
     * Counts how many songs use a specific cover art ID.
     *
     * @param coverArtId Target cover art ID.
     * @return Number of songs using the cover art.
     */
    @Query("SELECT COUNT(*) FROM offline_songs WHERE coverArt = :coverArtId")
    suspend fun countSongsWithCoverArt(coverArtId: String): Int
}
