package com.vsulimov.resonance.ui.screens.library.categories.albums

import com.vsulimov.libsubsonic.data.response.browsing.Album
import com.vsulimov.resonance.core.ui.UiText

/**
 * Immutable UI state for the Albums Category Screen.
 *
 * @property isLoading Indicates whether the initial page of albums is currently being fetched.
 * @property isPaginationLoading Indicates whether subsequent pages are currently being appended.
 * @property canPaginate Indicates whether more albums can be fetched from the server.
 * @property albums The full list of albums loaded so far from the server.
 * @property searchQuery Active search query string.
 * @property isSearchActive Whether search bar mode is expanded.
 * @property isGridMode Active layout view mode (true for grid, false for list).
 * @property error Localized [UiText] error representation if data fetching failed, or null if successful.
 */
data class AlbumsUiState(
    val isLoading: Boolean = true,
    val isPaginationLoading: Boolean = false,
    val canPaginate: Boolean = true,
    val albums: List<Album> = emptyList(),
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val isGridMode: Boolean = true,
    val error: UiText? = null
)
