package com.vsulimov.resonance.domain.model

/**
 * Full domain representation of an album including its track list.
 *
 * Used on the album detail screen where complete album information
 * and individual tracks are displayed. Mapped from the Subsonic
 * [Album][com.vsulimov.libsubsonic.data.response.browsing.Album]
 * response by [AlbumDetailMapper][com.vsulimov.resonance.data.mapper.AlbumDetailMapper].
 *
 * @param id Unique album identifier from the Subsonic server.
 * @param name Album title.
 * @param artistName Display name of the album's artist.
 * @param coverArtId Identifier for retrieving cover art, or `null` if unavailable.
 * @param year Release year, or `null` if unavailable.
 * @param genre Primary genre, or `null` if unavailable.
 * @param songCount Total number of songs in the album.
 * @param durationSeconds Total album duration in seconds, or `null` if unavailable.
 * @param tracks Ordered list of tracks in the album.
 */
data class AlbumDetail(
    val id: String,
    val name: String,
    val artistName: String,
    val coverArtId: String?,
    val year: Int?,
    val genre: String?,
    val songCount: Int,
    val durationSeconds: Int?,
    val tracks: List<TrackSummary>
)
