package com.vsulimov.resonance.data.mapper

import com.vsulimov.libsubsonic.data.response.browsing.Album
import com.vsulimov.resonance.domain.model.SearchAlbum

/**
 * Maps libsubsonic [Album] to the domain [SearchAlbum] model.
 *
 * Includes [songCount] and [year] fields that are not present in
 * [AlbumSummary][com.vsulimov.resonance.domain.model.AlbumSummary],
 * needed for the search results metadata line.
 */
object SearchAlbumMapper {

    private const val UNKNOWN_ARTIST = "Unknown Artist"

    /**
     * Converts a single [Album] into a [SearchAlbum].
     *
     * @param album The raw album data from the Subsonic API.
     * @return A domain model with [artistName] guaranteed non-null.
     */
    fun map(album: Album): SearchAlbum = SearchAlbum(
        id = album.id,
        name = album.name,
        artistName = album.artist ?: UNKNOWN_ARTIST,
        coverArtId = album.coverArt,
        songCount = album.songCount,
        year = album.year
    )

    /**
     * Converts a list of [Album] instances into [SearchAlbum] instances.
     *
     * @param albums The raw album list from the Subsonic API.
     * @return A list of domain models.
     */
    fun mapList(albums: List<Album>): List<SearchAlbum> = albums.map(::map)
}
