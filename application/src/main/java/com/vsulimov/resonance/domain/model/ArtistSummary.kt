package com.vsulimov.resonance.domain.model

/**
 * Lightweight representation of an artist for list display.
 *
 * Contains only the fields needed to render an artist card.
 * Mapped from the Subsonic `Artist` response model by
 * [ArtistSummaryMapper][com.vsulimov.resonance.data.mapper.ArtistSummaryMapper].
 *
 * @property id Unique artist identifier.
 * @property name Display name of the artist.
 * @property albumCount Number of albums by this artist.
 * @property coverArtId Identifier for loading artist cover art, or `null` if unavailable.
 */
data class ArtistSummary(
    val id: String,
    val name: String,
    val albumCount: Int,
    val coverArtId: String?
)
