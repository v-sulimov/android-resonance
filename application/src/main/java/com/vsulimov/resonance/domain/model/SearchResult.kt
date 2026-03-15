package com.vsulimov.resonance.domain.model

/**
 * Aggregated search results from the Subsonic `search3` API.
 *
 * Groups matched artists, albums, and songs into a single container
 * for presentation in the search results screen.
 *
 * @param artists Matched artists, ordered by relevance.
 * @param albums Matched albums, ordered by relevance.
 * @param songs Matched songs, ordered by relevance.
 */
data class SearchResult(
    val artists: List<ArtistSummary>,
    val albums: List<SearchAlbum>,
    val songs: List<SongSummary>
)

/**
 * Domain representation of an album within search results.
 *
 * Extends the basic [AlbumSummary] fields with [songCount] and [year]
 * needed for the search result metadata line (e.g. "Artist · 12 tracks · 1997").
 *
 * @param id Unique album identifier from the Subsonic server.
 * @param name Album title.
 * @param artistName Display name of the album's artist.
 * @param coverArtId Identifier for retrieving cover art, or `null` if unavailable.
 * @param songCount Number of tracks on the album, or `null` if unavailable.
 * @param year Release year, or `null` if unavailable.
 */
data class SearchAlbum(
    val id: String,
    val name: String,
    val artistName: String,
    val coverArtId: String?,
    val songCount: Int?,
    val year: Int?
)
