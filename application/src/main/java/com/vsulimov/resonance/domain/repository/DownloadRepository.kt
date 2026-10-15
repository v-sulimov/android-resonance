package com.vsulimov.resonance.domain.repository

import com.vsulimov.libsubsonic.data.response.browsing.Child
import com.vsulimov.resonance.data.db.entity.DownloadTaskEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing offline music downloads and batch queue resolvers.
 */
interface DownloadRepository {

    /**
     * Resumes background download service on app startup if unpaused tasks exist in queue.
     */
    fun resumeActiveDownloadsOnAppStart()

    /**
     * Enqueues a single song for offline download.
     *
     * @param song The song model to download.
     */
    suspend fun enqueueSong(song: Child)

    /**
     * Enqueues an entire album for offline download.
     *
     * @param albumId Unique identifier for target album.
     */
    suspend fun enqueueAlbum(albumId: String)

    /**
     * Enqueues an artist (all associated albums and tracks) for offline download.
     *
     * @param artistId Unique identifier for target artist.
     */
    suspend fun enqueueArtist(artistId: String)

    /**
     * Enqueues a playlist (metadata, cross-refs, and tracks) for offline download.
     *
     * @param playlistId Unique identifier for target playlist.
     */
    suspend fun enqueuePlaylist(playlistId: String)

    /**
     * Enqueues the entire server library for offline download by iteratively fetching all albums and tracks.
     */
    suspend fun enqueueEntireLibrary()

    /**
     * Cancels a specific download task and deletes any partial local audio file.
     *
     * @param songId Target song unique ID.
     */
    suspend fun cancelDownload(songId: String)

    /**
     * Cancels all queued and active download tasks and removes them from queue.
     */
    suspend fun cancelAllDownloads()

    /**
     * Removes all failed download tasks from the database.
     */
    suspend fun clearFailedDownloads()

    /**
     * Deletes a downloaded song from disk and database.
     *
     * @param songId Target song unique ID.
     */
    suspend fun deleteDownloadedSong(songId: String)

    /**
     * Deletes all downloaded songs for an album from disk and database.
     *
     * @param albumId Target album unique ID.
     */
    suspend fun deleteDownloadedAlbum(albumId: String)

    /**
     * Deletes all downloaded songs for an artist from disk and database.
     *
     * @param artistId Target artist unique ID.
     */
    suspend fun deleteDownloadedArtist(artistId: String)

    /**
     * Deletes a playlist entity, cross-references, and optionally downloaded songs.
     *
     * @param playlistId Target playlist unique ID.
     */
    suspend fun deleteDownloadedPlaylist(playlistId: String)

    /**
     * Deletes all downloaded audio files, permanent offline covers, and clears offline Room metadata.
     */
    suspend fun deleteAllDownloads()

    /**
     * Observes all download tasks ordered by queue time. Emits on every database change, including bytes downloaded.
     *
     * @return [Flow] emitting list of [DownloadTaskEntity].
     */
    fun observeAllTasks(): Flow<List<DownloadTaskEntity>>

    /**
     * Observes all download tasks, but simplifies the state by zeroing out bytesDownloaded to prevent
     * high-frequency emissions (which cause UI recompositions) when only progress is changing.
     *
     * @return [Flow] emitting list of simplified [DownloadTaskEntity].
     */
    fun observeAllTasksSimplified(): Flow<List<DownloadTaskEntity>>

    /**
     * Observes download task for a specific song ID.
     *
     * @param songId Target song unique ID.
     * @return [Flow] emitting task entity or null.
     */
    fun observeTaskBySongId(songId: String): Flow<DownloadTaskEntity?>

    /**
     * Observes count of queued and active downloading tasks.
     *
     * @return [Flow] emitting count of active tasks.
     */
    fun observeActiveTaskCount(): Flow<Int>
}
