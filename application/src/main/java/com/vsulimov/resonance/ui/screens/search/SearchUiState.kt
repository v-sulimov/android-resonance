package com.vsulimov.resonance.ui.screens.search

import com.vsulimov.libsubsonic.data.response.browsing.Album
import com.vsulimov.libsubsonic.data.response.browsing.Artist
import com.vsulimov.libsubsonic.data.response.browsing.Child
import com.vsulimov.resonance.core.ui.UiText

/**
 * Filter categories available on the Search screen.
 */
enum class SearchFilter {
    ALL,
    SONGS,
    ALBUMS,
    ARTISTS
}

/**
 * Immutable UI state for the Search Screen.
 *
 * @property searchQuery Active user search query.
 * @property selectedFilter Currently active search filter chip.
 * @property isLoading Indicates whether a search request is in progress.
 * @property songs Search result songs.
 * @property albums Search result albums.
 * @property artists Search result artists.
 * @property error Localized [UiText] error representation if search failed, or null if successful.
 */
data class SearchUiState(
    val searchQuery: String = "",
    val selectedFilter: SearchFilter = SearchFilter.ALL,
    val isLoading: Boolean = false,
    val songs: List<Child> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val error: UiText? = null
)
