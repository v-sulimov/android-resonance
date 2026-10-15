package com.vsulimov.resonance.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vsulimov.resonance.data.db.DownloadStatus

/**
 * Room entity tracking pending, active, and completed download tasks.
 *
 * @property id Primary key task ID (matches target song ID).
 * @property songId Target song unique ID.
 * @property status Current download progress state.
 * @property bytesDownloaded Number of bytes fetched so far.
 * @property totalBytes Expected total file size in bytes.
 * @property errorMessage Error description if download failed.
 * @property addedAt Timestamp when download task was queued.
 * @property updatedAt Timestamp when task progress was last updated.
 */
@Entity(
    tableName = "download_tasks",
    indices = [
        Index(value = ["songId"]),
        Index(value = ["status"])
    ]
)
data class DownloadTaskEntity(
    @PrimaryKey
    val id: String,
    val songId: String,
    val status: DownloadStatus,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val errorMessage: String?,
    val addedAt: Long,
    val updatedAt: Long
)
