package com.vsulimov.resonance.domain.model

/**
 * Domain representation of a song for use in the songs list.
 *
 * Unlike [TrackSummary] (used in album detail where album context is implicit),
 * this model includes album metadata needed to display songs outside an album context.
 *
 * @param id Unique track identifier from the Subsonic server.
 * @param title Track display title.
 * @param artistName Display name of the track's artist.
 * @param albumName Display name of the album containing this track.
 * @param albumId Album identifier for navigation to album detail.
 * @param coverArtId Cover art identifier for thumbnail loading, or `null` if unavailable.
 * @param durationSeconds Track duration in seconds, or `null` if unavailable.
 */
data class SongSummary(
    val id: String,
    val title: String,
    val artistName: String,
    val albumName: String,
    val albumId: String?,
    val coverArtId: String?,
    val durationSeconds: Int?
)
