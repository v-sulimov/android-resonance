package com.vsulimov.resonance.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vsulimov.resonance.data.db.DownloadStatus
import com.vsulimov.resonance.data.db.entity.DownloadTaskEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for managing download tasks in Room database.
 */
@Dao
interface DownloadTaskDao {

    /**
     * Fetches a task entity by unique ID.
     *
     * @param id Target task ID.
     * @return [DownloadTaskEntity] or null if not found.
     */
    @Query("SELECT * FROM download_tasks WHERE id = :id")
    suspend fun getTaskById(id: String): DownloadTaskEntity?

    /**
     * Fetches a task entity by target song ID.
     *
     * @param songId Target song ID.
     * @return [DownloadTaskEntity] or null if not found.
     */
    @Query("SELECT * FROM download_tasks WHERE songId = :songId")
    suspend fun getTaskBySongId(songId: String): DownloadTaskEntity?

    /**
     * Observes a task entity by target song ID.
     *
     * @param songId Target song ID.
     * @return [Flow] emitting download task updates or null.
     */
    @Query("SELECT * FROM download_tasks WHERE songId = :songId")
    fun observeTaskBySongId(songId: String): Flow<DownloadTaskEntity?>

    /**
     * Observes all download tasks ordered by queue time ascending.
     *
     * @return [Flow] emitting list of all download tasks.
     */
    @Query("SELECT * FROM download_tasks ORDER BY addedAt ASC")
    fun observeAllTasks(): Flow<List<DownloadTaskEntity>>

    /**
     * Observes download tasks filtering by status ordered by queue time ascending.
     *
     * @param status Target status filter.
     * @return [Flow] emitting filtered download tasks.
     */
    @Query("SELECT * FROM download_tasks WHERE status = :status ORDER BY addedAt ASC")
    fun observeTasksByStatus(status: DownloadStatus): Flow<List<DownloadTaskEntity>>

    /**
     * Retrieves download tasks filtering by status ordered by queue time ascending.
     *
     * @param status Target status filter.
     * @return Filtered list of download tasks.
     */
    @Query("SELECT * FROM download_tasks WHERE status = :status ORDER BY addedAt ASC")
    suspend fun getTasksByStatus(status: DownloadStatus): List<DownloadTaskEntity>

    /**
     * Observes task count matching target status.
     *
     * @param status Target status filter.
     * @return [Flow] emitting count of matching tasks.
     */
    @Query("SELECT COUNT(*) FROM download_tasks WHERE status = :status")
    fun observeTaskCountByStatus(status: DownloadStatus): Flow<Int>

    /**
     * Inserts or replaces a download task entity.
     *
     * @param task Task entity to save.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: DownloadTaskEntity)

    /**
     * Inserts or replaces a list of download task entities.
     *
     * @param tasks List of task entities to save.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<DownloadTaskEntity>)

    /**
     * Updates an existing download task entity.
     *
     * @param task Task entity with updated values.
     */
    @Update
    suspend fun updateTask(task: DownloadTaskEntity)

    /**
     * Updates bytes progress and status for a download task.
     *
     * @param id Target task ID.
     * @param status Updated status.
     * @param bytesDownloaded Current byte count downloaded.
     * @param updatedAt Timestamp of update.
     */
    @Query(
        "UPDATE download_tasks SET status = :status, bytesDownloaded = :bytesDownloaded, updatedAt = :updatedAt WHERE id = :id"
    )
    suspend fun updateProgress(id: String, status: DownloadStatus, bytesDownloaded: Long, updatedAt: Long)

    /**
     * Updates status and optional error message for a download task.
     *
     * @param id Target task ID.
     * @param status Updated status.
     * @param errorMessage Error description or null.
     * @param updatedAt Timestamp of update.
     */
    @Query(
        "UPDATE download_tasks SET status = :status, errorMessage = :errorMessage, updatedAt = :updatedAt WHERE id = :id"
    )
    suspend fun updateStatusAndError(id: String, status: DownloadStatus, errorMessage: String?, updatedAt: Long)

    /**
     * Deletes a download task entity.
     *
     * @param task Task entity to delete.
     */
    @Delete
    suspend fun deleteTask(task: DownloadTaskEntity)

    /**
     * Deletes a download task by ID.
     *
     * @param id Target task ID.
     */
    @Query("DELETE FROM download_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: String)

    /**
     * Deletes all tasks matching specified status.
     *
     * @param status Status to delete.
     */
    @Query("DELETE FROM download_tasks WHERE status = :status")
    suspend fun deleteTasksByStatus(status: DownloadStatus)

    /**
     * Clears all download tasks from database.
     */
    @Query("DELETE FROM download_tasks")
    suspend fun clearAll()
}
