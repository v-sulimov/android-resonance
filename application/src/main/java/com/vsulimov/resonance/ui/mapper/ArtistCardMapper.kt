package com.vsulimov.resonance.ui.mapper

import com.vsulimov.resonance.domain.model.ArtistSummary
import com.vsulimov.resonance.ui.screen.artistlist.ArtistCardViewObject

/**
 * Maps domain [ArtistSummary] to presentation [ArtistCardViewObject].
 */
object ArtistCardMapper {

    /**
     * Converts a single [ArtistSummary] into an [ArtistCardViewObject].
     *
     * @param artist The domain model to convert.
     * @return A view object ready for UI rendering.
     */
    fun map(artist: ArtistSummary): ArtistCardViewObject = ArtistCardViewObject(
        id = artist.id,
        name = artist.name,
        albumCount = artist.albumCount,
        coverArtId = artist.coverArtId
    )

    /**
     * Converts a list of [ArtistSummary] instances into [ArtistCardViewObject] instances.
     *
     * @param artists The domain models to convert.
     * @return A list of view objects ready for UI rendering.
     */
    fun mapList(artists: List<ArtistSummary>): List<ArtistCardViewObject> = artists.map(::map)
}
