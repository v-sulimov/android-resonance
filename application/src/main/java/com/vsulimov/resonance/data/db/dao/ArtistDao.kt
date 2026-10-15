package com.vsulimov.resonance.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vsulimov.resonance.data.db.entity.OfflineArtistEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for managing offline artists in Room database.
 */
@Dao
interface ArtistDao {

    /**
     * Fetches an artist entity by unique ID.
     *
     * @param id Target artist ID.
     * @return [OfflineArtistEntity] or null if not found.
     */
    @Query("SELECT * FROM offline_artists WHERE id = :id")
    suspend fun getArtistById(id: String): OfflineArtistEntity?

    /**
     * Observes an artist entity by ID.
     *
     * @param id Target artist ID.
     * @return [Flow] emitting artist updates or null.
     */
    @Query("SELECT * FROM offline_artists WHERE id = :id")
    fun observeArtistById(id: String): Flow<OfflineArtistEntity?>

    /**
     * Observes all offline artists sorted alphabetically by name.
     *
     * @return [Flow] emitting list of all artists.
     */
    @Query("SELECT * FROM offline_artists ORDER BY name ASC")
    fun observeAllArtists(): Flow<List<OfflineArtistEntity>>

    /**
     * Observes all starred/favorited offline artists.
     *
     * @return [Flow] emitting list of starred artists.
     */
    @Query("SELECT * FROM offline_artists WHERE isStarred = 1 ORDER BY name ASC")
    fun observeStarredArtists(): Flow<List<OfflineArtistEntity>>

    /**
     * Searches offline artists matching query against name.
     *
     * @param query Search string.
     * @return [Flow] emitting matching artists.
     */
    @Query("SELECT * FROM offline_artists WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchArtists(query: String): Flow<List<OfflineArtistEntity>>

    /**
     * Inserts or replaces an artist entity.
     *
     * @param artist Artist entity to save.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtist(artist: OfflineArtistEntity)

    /**
     * Inserts or replaces a list of artist entities.
     *
     * @param artists List of artist entities to save.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtists(artists: List<OfflineArtistEntity>)

    /**
     * Updates an existing artist entity.
     *
     * @param artist Artist entity with updated values.
     */
    @Update
    suspend fun updateArtist(artist: OfflineArtistEntity)

    /**
     * Updates starred status of a target artist.
     *
     * @param id Target artist ID.
     * @param isStarred New starred state.
     */
    @Query("UPDATE offline_artists SET isStarred = :isStarred WHERE id = :id")
    suspend fun updateArtistStarred(id: String, isStarred: Boolean)

    /**
     * Deletes an artist entity.
     *
     * @param artist Artist entity to delete.
     */
    @Delete
    suspend fun deleteArtist(artist: OfflineArtistEntity)

    /**
     * Deletes an artist entity by ID.
     *
     * @param id Target artist ID.
     */
    @Query("DELETE FROM offline_artists WHERE id = :id")
    suspend fun deleteArtistById(id: String)

    /**
     * Clears all artists from database.
     */
    @Query("DELETE FROM offline_artists")
    suspend fun clearAll()

    /**
     * Counts how many artists use a specific cover art ID.
     *
     * @param coverArtId Target cover art ID.
     * @return Number of artists using the cover art.
     */
    @Query("SELECT COUNT(*) FROM offline_artists WHERE coverArt = :coverArtId")
    suspend fun countArtistsWithCoverArt(coverArtId: String): Int
}
