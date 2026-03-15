package com.vsulimov.resonance.ui.screen.songlist

/**
 * View object representing a single song in the songs list.
 *
 * Carries structured data so composables can format using localized
 * string resources.
 *
 * @param id Unique song identifier.
 * @param title Song display title.
 * @param artistName Artist display name.
 * @param albumName Album display name.
 * @param albumId Album identifier for navigation to album detail, or `null`.
 * @param coverArtId Cover art identifier for thumbnail loading, or `null`.
 * @param durationSeconds Duration in seconds for formatting, or `null`.
 */
data class SongListItemViewObject(
    val id: String,
    val title: String,
    val artistName: String,
    val albumName: String,
    val albumId: String?,
    val coverArtId: String?,
    val durationSeconds: Int?
)
