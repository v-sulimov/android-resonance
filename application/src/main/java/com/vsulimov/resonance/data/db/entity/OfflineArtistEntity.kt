package com.vsulimov.resonance.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing an offline music artist.
 *
 * @property id Unique identifier for the artist.
 * @property name Artist name.
 * @property coverArt Cover art identifier for loading image.
 * @property albumCount Number of albums associated with artist.
 * @property isStarred Whether the artist is starred/favorited.
 */
@Entity(tableName = "offline_artists")
data class OfflineArtistEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val coverArt: String?,
    val albumCount: Int,
    val isStarred: Boolean
)
