package com.vsulimov.resonance.ui.screen.main.mix

/**
 * Presentation-layer data class for a single album card in a carousel.
 *
 * Contains only the fields needed to render the card UI. Mapped from
 * the domain [AlbumSummary][com.vsulimov.resonance.domain.model.AlbumSummary]
 * by [AlbumCardMapper][com.vsulimov.resonance.ui.mapper.AlbumCardMapper].
 *
 * @param id Unique album identifier (used for click navigation).
 * @param name Album title displayed below the cover art.
 * @param artistName Artist name displayed below the album title.
 * @param coverArtId Identifier for loading cover art, or `null` if unavailable.
 */
data class AlbumCardViewObject(
    val id: String,
    val name: String,
    val artistName: String,
    val coverArtId: String?
)
