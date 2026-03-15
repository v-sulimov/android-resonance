package com.vsulimov.resonance.ui.screen.artistlist

/**
 * Presentation-layer data class for a single artist card in the list.
 *
 * Contains only the fields needed to render the card UI. Mapped from
 * the domain [ArtistSummary][com.vsulimov.resonance.domain.model.ArtistSummary]
 * by [ArtistCardMapper][com.vsulimov.resonance.ui.mapper.ArtistCardMapper].
 *
 * @param id Unique artist identifier (used for click navigation).
 * @param name Artist display name.
 * @param albumCount Number of albums by this artist.
 * @param coverArtId Identifier for loading artist cover art, or `null` if unavailable.
 */
data class ArtistCardViewObject(
    val id: String,
    val name: String,
    val albumCount: Int,
    val coverArtId: String?
)
