package com.vsulimov.resonance.data.mapper

import com.vsulimov.libsubsonic.data.response.browsing.Album
import com.vsulimov.resonance.domain.model.AlbumSummary

/**
 * Maps libsubsonic [Album] to the domain [AlbumSummary] model.
 *
 * Handles nullable fields from the API by falling back to sensible defaults
 * so the domain layer always works with non-null values where expected.
 */
object AlbumSummaryMapper {

    private const val UNKNOWN_ARTIST = "Unknown Artist"

    /**
     * Converts a single [Album] into an [AlbumSummary].
     *
     * @param album The raw album data from the Subsonic API.
     * @return A domain model with [artistName] guaranteed non-null.
     */
    fun map(album: Album): AlbumSummary = AlbumSummary(
        id = album.id,
        name = album.name,
        artistName = album.artist ?: UNKNOWN_ARTIST,
        coverArtId = album.coverArt
    )

    /**
     * Converts a list of [Album] instances into [AlbumSummary] instances.
     *
     * @param albums The raw album list from the Subsonic API.
     * @return A list of domain models.
     */
    fun mapList(albums: List<Album>): List<AlbumSummary> = albums.map(::map)
}
