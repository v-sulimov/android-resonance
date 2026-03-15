package com.vsulimov.resonance.domain.model

/**
 * Aggregated library category counts fetched from the music server.
 *
 * Each field is an independent [Result] so that a failure in one
 * API call does not prevent the others from displaying. The
 * presentation layer filters out failed results and shows whichever
 * counts succeed.
 *
 * @property artistCount Total number of artists in the library.
 * @property albumCount Total number of albums in the library.
 * @property songCount Total number of songs (tracks) in the library.
 * @property playlistCount Total number of user playlists.
 * @property genreCount Total number of distinct genres.
 * @property favoriteTrackCount Total number of starred/favorited tracks.
 */
data class LibraryCounts(
    val artistCount: Result<Int>,
    val albumCount: Result<Int>,
    val songCount: Result<Int>,
    val playlistCount: Result<Int>,
    val genreCount: Result<Int>,
    val favoriteTrackCount: Result<Int>
)
