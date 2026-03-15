package com.vsulimov.resonance.domain.model

/**
 * Domain representation of a single track within an album.
 *
 * Contains the fields needed for the album detail track list.
 * Mapped from the Subsonic [Child][com.vsulimov.libsubsonic.data.response.browsing.Child]
 * response by [TrackSummaryMapper][com.vsulimov.resonance.data.mapper.TrackSummaryMapper].
 *
 * @param id Unique track identifier from the Subsonic server.
 * @param title Track display title.
 * @param artistName Display name of the track's artist.
 * @param trackNumber Position of the track within the album, or `null` if unavailable.
 * @param durationSeconds Track duration in seconds, or `null` if unavailable.
 */
data class TrackSummary(
    val id: String,
    val title: String,
    val artistName: String,
    val trackNumber: Int?,
    val durationSeconds: Int?
)
