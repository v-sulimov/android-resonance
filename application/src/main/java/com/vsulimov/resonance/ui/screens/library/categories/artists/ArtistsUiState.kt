package com.vsulimov.resonance.ui.screens.library.categories.artists

import com.vsulimov.libsubsonic.data.response.browsing.ArtistIndex
import com.vsulimov.resonance.R
import com.vsulimov.resonance.core.ui.UiText
import com.vsulimov.resonance.ui.components.SortOption

/**
 * Sorting criteria for the Artists screen.
 */
enum class ArtistSortCriteria {
    NAME,
    ALBUM_COUNT
}

/**
 * Available sorting options for the Artists category.
 */
val ARTIST_SORT_OPTIONS = listOf(
    SortOption(ArtistSortCriteria.NAME, R.string.sort_name),
    SortOption(ArtistSortCriteria.ALBUM_COUNT, R.string.sort_song_count)
)

/**
 * Immutable UI state for the Artists Category Screen.
 *
 * @property isLoading Indicates whether artist data is currently being fetched.
 * @property artistIndexes Raw list of artist index groupings returned by the server.
 * @property filteredArtistIndexes Computed artist index groupings after filtering and sorting.
 * @property searchQuery Active search query string.
 * @property isSearchActive Whether search input mode is active.
 * @property selectedSort Active sort option.
 * @property isSortAscending Sort direction flag (true for ascending, false for descending).
 * @property isSortSheetOpen Whether the sort bottom sheet is currently open.
 * @property error Localized [UiText] error representation if data fetching failed, or null if successful.
 */
data class ArtistsUiState(
    val isLoading: Boolean = true,
    val artistIndexes: List<ArtistIndex> = emptyList(),
    val filteredArtistIndexes: List<ArtistIndex> = emptyList(),
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val selectedSort: SortOption<ArtistSortCriteria> = ARTIST_SORT_OPTIONS.first(),
    val isSortAscending: Boolean = true,
    val isSortSheetOpen: Boolean = false,
    val error: UiText? = null
)
