package com.vsulimov.resonance.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing an offline music track/song.
 *
 * @property id Unique identifier for the song from Subsonic server.
 * @property title Name of the song track.
 * @property album Optional album title.
 * @property albumId Optional album unique ID.
 * @property artist Optional artist name.
 * @property artistId Optional artist unique ID.
 * @property track Track number within the album.
 * @property discNumber Disc number within multi-disc sets.
 * @property year Release year.
 * @property genre Music genre.
 * @property coverArt Cover art identifier for loading album art.
 * @property size File size in bytes.
 * @property contentType MIME content type.
 * @property suffix Audio file extension (e.g., mp3, flac).
 * @property duration Track duration in seconds.
 * @property bitRate Audio bitrate in kbps.
 * @property path Remote file path on server.
 * @property localFilePath Absolute local disk path to downloaded audio file.
 * @property isStarred Whether the song is starred/favorited by user.
 * @property downloadedAt System timestamp when download completed.
 */
@Entity(
    tableName = "offline_songs",
    indices = [
        Index(value = ["albumId"]),
        Index(value = ["artistId"])
    ]
)
data class OfflineSongEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val album: String?,
    val albumId: String?,
    val artist: String?,
    val artistId: String?,
    val track: Int?,
    val discNumber: Int?,
    val year: Int?,
    val genre: String?,
    val coverArt: String?,
    val size: Long?,
    val contentType: String?,
    val suffix: String?,
    val duration: Int,
    val bitRate: Int?,
    val path: String?,
    val localFilePath: String?,
    val isStarred: Boolean,
    val downloadedAt: Long
)
