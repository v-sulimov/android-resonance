package com.vsulimov.resonance.data.mapper

import com.vsulimov.libsubsonic.data.response.browsing.Artist
import com.vsulimov.libsubsonic.data.response.browsing.ArtistIndex
import com.vsulimov.resonance.domain.model.ArtistSummary

/**
 * Maps libsubsonic [Artist] to the domain [ArtistSummary] model.
 */
object ArtistSummaryMapper {

    /**
     * Converts a single [Artist] into an [ArtistSummary].
     *
     * @param artist The raw artist data from the Subsonic API.
     * @return A domain model ready for the presentation layer.
     */
    fun map(artist: Artist): ArtistSummary = ArtistSummary(
        id = artist.id,
        name = artist.name,
        albumCount = artist.albumCount,
        coverArtId = artist.coverArt
    )

    /**
     * Flattens a list of [ArtistIndex] groups into a single list of [ArtistSummary].
     *
     * The server returns artists grouped alphabetically by index letter.
     * This method flattens all groups into a single ordered list.
     *
     * @param indices The alphabetical artist index groups from the Subsonic API.
     * @return A flat list of domain models in alphabetical order.
     */
    fun mapFromIndices(indices: List<ArtistIndex>): List<ArtistSummary> =
        indices.flatMap { index -> index.artists.map(::map) }
}
