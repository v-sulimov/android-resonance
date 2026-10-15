package com.vsulimov.resonance.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing an offline music album.
 *
 * @property id Unique identifier for the album.
 * @property name Album title.
 * @property artist Primary artist name.
 * @property artistId Artist unique ID.
 * @property coverArt Cover art identifier for loading artwork.
 * @property songCount Total number of tracks in album.
 * @property duration Aggregate album duration in seconds.
 * @property year Release year.
 * @property genre Music genre.
 * @property created ISO creation timestamp.
 * @property isStarred Whether the album is starred/favorited.
 */
@Entity(
    tableName = "offline_albums",
    indices = [
        Index(value = ["artistId"])
    ]
)
data class OfflineAlbumEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val artist: String?,
    val artistId: String?,
    val coverArt: String?,
    val songCount: Int,
    val duration: Int,
    val year: Int?,
    val genre: String?,
    val created: String?,
    val isStarred: Boolean
)
