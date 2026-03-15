package com.vsulimov.resonance.domain.model

/**
 * Lightweight domain representation of an album for list and carousel display.
 *
 * Contains only the fields needed for album cards — full album details
 * (songs, duration, year, etc.) are loaded separately when the user
 * navigates to an album detail screen.
 *
 * @param id Unique album identifier from the Subsonic server.
 * @param name Album title.
 * @param artistName Display name of the album's artist.
 * @param coverArtId Identifier for retrieving cover art via the cover art endpoint,
 *   or `null` if the album has no associated artwork.
 */
data class AlbumSummary(
    val id: String,
    val name: String,
    val artistName: String,
    val coverArtId: String?
)
