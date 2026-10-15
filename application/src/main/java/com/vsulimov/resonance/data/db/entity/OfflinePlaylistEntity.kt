package com.vsulimov.resonance.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing an offline playlist.
 *
 * @property id Unique identifier for the playlist.
 * @property name Playlist title.
 * @property comment User notes or playlist description.
 * @property owner Username of playlist creator.
 * @property public Whether playlist is visible to other server users.
 * @property songCount Number of tracks in playlist.
 * @property duration Total duration in seconds.
 * @property created ISO creation timestamp.
 * @property changed ISO modification timestamp.
 * @property coverArt Cover art identifier for playlist artwork.
 */
@Entity(tableName = "offline_playlists")
data class OfflinePlaylistEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val comment: String?,
    val owner: String?,
    val public: Boolean?,
    val songCount: Int,
    val duration: Int,
    val created: String?,
    val changed: String?,
    val coverArt: String?
)
