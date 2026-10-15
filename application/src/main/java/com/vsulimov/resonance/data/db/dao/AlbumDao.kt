package com.vsulimov.resonance.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vsulimov.resonance.data.db.entity.OfflineAlbumEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for managing offline albums in Room database.
 */
@Dao
interface AlbumDao {

    /**
     * Fetches an album entity by its unique ID.
     *
     * @param id Target album ID.
     * @return [OfflineAlbumEntity] or null if not found.
     */
    @Query("SELECT * FROM offline_albums WHERE id = :id")
    suspend fun getAlbumById(id: String): OfflineAlbumEntity?

    /**
     * Observes an album entity by ID.
     *
     * @param id Target album ID.
     * @return [Flow] emitting album updates or null.
     */
    @Query("SELECT * FROM offline_albums WHERE id = :id")
    fun observeAlbumById(id: String): Flow<OfflineAlbumEntity?>

    /**
     * Observes all offline albums sorted alphabetically by name.
     *
     * @return [Flow] emitting list of all albums.
     */
    @Query("SELECT * FROM offline_albums ORDER BY name ASC")
    fun observeAllAlbums(): Flow<List<OfflineAlbumEntity>>

    /**
     * Observes albums associated with a specific artist sorted by release year descending and name.
     *
     * @param artistId Target artist ID.
     * @return [Flow] emitting list of albums for specified artist.
     */
    @Query("SELECT * FROM offline_albums WHERE artistId = :artistId ORDER BY year DESC, name ASC")
    fun observeAlbumsByArtistId(artistId: String): Flow<List<OfflineAlbumEntity>>

    @Query("SELECT * FROM offline_albums WHERE artistId = :artistId ORDER BY year DESC, name ASC")
    suspend fun getAlbumsByArtistId(artistId: String): List<OfflineAlbumEntity>

    /**
     * Observes all starred/favorited offline albums.
     *
     * @return [Flow] emitting list of starred albums.
     */
    @Query("SELECT * FROM offline_albums WHERE isStarred = 1 ORDER BY name ASC")
    fun observeStarredAlbums(): Flow<List<OfflineAlbumEntity>>

    /**
     * Searches offline albums matching query against name or artist.
     *
     * @param query Search string.
     * @return [Flow] emitting matching albums.
     */
    @Query(
        "SELECT * FROM offline_albums WHERE name LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' ORDER BY name ASC"
    )
    fun searchAlbums(query: String): Flow<List<OfflineAlbumEntity>>

    /**
     * Inserts or replaces an album entity.
     *
     * @param album Album entity to save.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbum(album: OfflineAlbumEntity)

    /**
     * Inserts or replaces a list of album entities.
     *
     * @param albums List of album entities to save.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbums(albums: List<OfflineAlbumEntity>)

    /**
     * Updates an existing album entity.
     *
     * @param album Album entity with updated values.
     */
    @Update
    suspend fun updateAlbum(album: OfflineAlbumEntity)

    /**
     * Updates starred status of a target album.
     *
     * @param id Target album ID.
     * @param isStarred New starred state.
     */
    @Query("UPDATE offline_albums SET isStarred = :isStarred WHERE id = :id")
    suspend fun updateAlbumStarred(id: String, isStarred: Boolean)

    /**
     * Deletes an album entity.
     *
     * @param album Album entity to delete.
     */
    @Delete
    suspend fun deleteAlbum(album: OfflineAlbumEntity)

    @Query("DELETE FROM offline_albums WHERE id = :id")
    suspend fun deleteAlbumById(id: String)

    /**
     * Clears all albums from database.
     */
    @Query("DELETE FROM offline_albums")
    suspend fun clearAll()

    /**
     * Counts how many albums use a specific cover art ID.
     *
     * @param coverArtId Target cover art ID.
     * @return Number of albums using the cover art.
     */
    @Query("SELECT COUNT(*) FROM offline_albums WHERE coverArt = :coverArtId")
    suspend fun countAlbumsWithCoverArt(coverArtId: String): Int
}
