package com.vsulimov.resonance.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Room cross-reference entity mapping playlist entries to offline songs with order position.
 *
 * @property playlistId Target playlist unique ID.
 * @property songId Target song unique ID.
 * @property position 0-based index position of song within playlist sequence.
 */
@Entity(
    tableName = "playlist_song_cross_ref",
    primaryKeys = ["playlistId", "songId", "position"],
    foreignKeys = [
        ForeignKey(
            entity = OfflinePlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["playlistId"]),
        Index(value = ["songId"])
    ]
)
data class PlaylistSongCrossRefEntity(val playlistId: String, val songId: String, val position: Int)
