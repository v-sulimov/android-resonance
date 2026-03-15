package com.vsulimov.resonance.ui.screen.main.search

/**
 * View object for an artist in search results.
 *
 * @param id Artist identifier for navigation.
 * @param name Display name of the artist.
 * @param albumCount Number of albums, shown in metadata line.
 * @param coverArtId Cover art identifier for thumbnail loading, or `null`.
 */
data class SearchArtistViewObject(
    val id: String,
    val name: String,
    val albumCount: Int,
    val coverArtId: String?
)

/**
 * View object for an album in search results.
 *
 * @param id Album identifier for navigation.
 * @param name Album title.
 * @param artistName Display name of the album's artist.
 * @param coverArtId Cover art identifier for thumbnail loading, or `null`.
 * @param songCount Number of tracks, or `null` if unavailable.
 * @param year Release year, or `null` if unavailable.
 */
data class SearchAlbumViewObject(
    val id: String,
    val name: String,
    val artistName: String,
    val coverArtId: String?,
    val songCount: Int?,
    val year: Int?
)

/**
 * View object for a song in search results.
 *
 * @param id Song identifier.
 * @param title Song title.
 * @param artistName Display name of the artist.
 * @param albumName Display name of the album.
 * @param albumId Album identifier for navigation, or `null`.
 * @param coverArtId Cover art identifier for thumbnail loading, or `null`.
 */
data class SearchSongViewObject(
    val id: String,
    val title: String,
    val artistName: String,
    val albumName: String,
    val albumId: String?,
    val coverArtId: String?
)
