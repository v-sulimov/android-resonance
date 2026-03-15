package com.vsulimov.resonance.ui.screen.albumdetail

/**
 * Presentation-layer data class for the album detail screen header.
 *
 * Contains pre-formatted display strings for album metadata and
 * the list of tracks. Mapped from the domain
 * [AlbumDetail][com.vsulimov.resonance.domain.model.AlbumDetail]
 * by [AlbumDetailViewObjectMapper][com.vsulimov.resonance.ui.mapper.AlbumDetailViewObjectMapper].
 *
 * @param id Unique album identifier.
 * @param name Album title.
 * @param artistName Artist display name.
 * @param coverArtId Identifier for loading cover art, or `null` if unavailable.
 * @param year Release year, or `null` if unavailable.
 * @param songCount Number of songs in the album, or `null` if zero/unavailable.
 * @param durationSeconds Total album duration in seconds, or `null` if unavailable.
 * @param tracks Ordered list of track view objects.
 */
data class AlbumDetailViewObject(
    val id: String,
    val name: String,
    val artistName: String,
    val coverArtId: String?,
    val year: Int?,
    val songCount: Int?,
    val durationSeconds: Int?,
    val tracks: List<TrackViewObject>
)
