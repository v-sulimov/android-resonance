package com.vsulimov.resonance.core.utils

import com.vsulimov.resonance.data.db.DownloadStatus
import com.vsulimov.resonance.data.db.entity.DownloadTaskEntity
import com.vsulimov.resonance.data.storage.OfflineStorageManager

/**
 * Helper utility functions for resolving download statuses and progress across UI components.
 */
object DownloadUtils {

    /**
     * Resolves the current [DownloadStatus] and progress ratio for a song ID.
     *
     * @param songId Target song unique ID.
     * @param suffix Optional file extension.
     * @param downloadTasks List of active or historical download tasks.
     * @param storageManager [OfflineStorageManager] instance for checking disk files.
     * @return Pair of [DownloadStatus] and progress ratio Float (0f to 1f).
     */
    fun resolveSongDownloadState(
        songId: String,
        suffix: String?,
        downloadTasks: List<DownloadTaskEntity>,
        storageManager: OfflineStorageManager
    ): Pair<DownloadStatus, Float> {
        val task = downloadTasks.firstOrNull { it.songId == songId }
        if (task != null) {
            if (task.status == DownloadStatus.DOWNLOADED) {
                val isFileOnDisk = storageManager.hasAudioFile(songId, suffix)
                if (!isFileOnDisk) {
                    return Pair(DownloadStatus.NOT_DOWNLOADED, 0f)
                }
                return Pair(DownloadStatus.DOWNLOADED, 1f)
            }
            val denominator = maxOf(task.totalBytes, task.bytesDownloaded)
            val progress = if (denominator > 0L) {
                (task.bytesDownloaded.toFloat() / denominator).coerceIn(0f, 1f)
            } else {
                0f
            }
            return Pair(task.status, progress)
        }
        val isFileOnDisk = storageManager.hasAudioFile(songId, suffix)
        val status = if (isFileOnDisk) DownloadStatus.DOWNLOADED else DownloadStatus.NOT_DOWNLOADED
        val progress = if (isFileOnDisk) 1f else 0f
        return Pair(status, progress)
    }

    /**
     * Resolves composite [DownloadStatus] for a group of song IDs (e.g. album or playlist).
     *
     * @param songIds List of known song IDs in collection.
     * @param downloadTasks List of active download tasks.
     * @param storageManager [OfflineStorageManager] instance.
     * @param expectedTotalCount Total expected song count in collection if known.
     * @return Composite [DownloadStatus].
     */
    fun resolveCollectionDownloadStatus(
        songIds: List<String>,
        downloadTasks: List<DownloadTaskEntity>,
        storageManager: OfflineStorageManager,
        expectedTotalCount: Int = songIds.size
    ): DownloadStatus {
        if (songIds.isEmpty()) return DownloadStatus.NOT_DOWNLOADED

        val targetCount = if (expectedTotalCount > 0) expectedTotalCount else songIds.size

        val states = songIds.map { id ->
            resolveSongDownloadState(id, null, downloadTasks, storageManager).first
        }

        val downloadedCount = states.count { it == DownloadStatus.DOWNLOADED }
        val isFullyDownloaded = songIds.size >= targetCount && downloadedCount >= targetCount

        return when {
            isFullyDownloaded -> DownloadStatus.DOWNLOADED
            states.any { it == DownloadStatus.DOWNLOADING } -> DownloadStatus.DOWNLOADING
            states.any { it == DownloadStatus.QUEUED } -> DownloadStatus.QUEUED
            states.any { it == DownloadStatus.FAILED } -> DownloadStatus.FAILED
            else -> DownloadStatus.NOT_DOWNLOADED
        }
    }

    /**
     * Checks if at least one song in the specified collection is downloaded or actively downloading/queued.
     *
     * @param songIds List of song IDs in collection.
     * @param downloadTasks List of active or historical download tasks.
     * @param storageManager [OfflineStorageManager] instance.
     * @return True if at least one song is saved or in download queue.
     */
    fun hasDownloadedOrActiveSongs(
        songIds: List<String>,
        downloadTasks: List<DownloadTaskEntity>,
        storageManager: OfflineStorageManager
    ): Boolean {
        if (songIds.isEmpty()) return false
        return songIds.any { id ->
            val status = resolveSongDownloadState(id, null, downloadTasks, storageManager).first
            status == DownloadStatus.DOWNLOADED || status == DownloadStatus.DOWNLOADING ||
                status == DownloadStatus.QUEUED
        }
    }

    /**
     * Calculates overall progress ratio (0f to 1f) for active download tasks in queue (QUEUED and DOWNLOADING).
     *
     * @param tasks List of download tasks.
     * @return Progress ratio between 0f and 1f.
     */
    fun calculateQueueProgress(tasks: List<DownloadTaskEntity>): Float {
        val activeTasks = tasks.filter {
            it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.DOWNLOADING
        }
        if (activeTasks.isEmpty()) return 0f

        val totalBytesSum = activeTasks.sumOf { maxOf(it.totalBytes, it.bytesDownloaded) }
        if (totalBytesSum > 0L) {
            val downloadedBytesSum = activeTasks.sumOf { it.bytesDownloaded }
            return (downloadedBytesSum.toFloat() / totalBytesSum).coerceIn(0f, 1f)
        }

        val totalProgressSum = activeTasks.sumOf { task ->
            val denominator = maxOf(task.totalBytes, task.bytesDownloaded)
            if (denominator > 0L) {
                (task.bytesDownloaded.toFloat() / denominator).coerceIn(0f, 1f).toDouble()
            } else {
                0.0
            }
        }
        return (totalProgressSum.toFloat() / activeTasks.size).coerceIn(0f, 1f)
    }
}
