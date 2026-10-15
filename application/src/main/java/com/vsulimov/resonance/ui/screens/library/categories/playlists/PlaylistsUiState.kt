package com.vsulimov.resonance.ui.screens.library.categories.playlists

import com.vsulimov.libsubsonic.data.response.playlists.Playlist
import com.vsulimov.resonance.R
import com.vsulimov.resonance.core.ui.UiText
import com.vsulimov.resonance.data.db.DownloadStatus
import com.vsulimov.resonance.ui.components.SortOption

/**
 * Sorting criteria for the Playlists screen.
 */
enum class PlaylistSortCriteria {
    NAME,
    SONG_COUNT,
    DURATION
}

/**
 * Available sorting options for the Playlists category.
 */
val PLAYLIST_SORT_OPTIONS = listOf(
    SortOption(PlaylistSortCriteria.NAME, R.string.sort_name),
    SortOption(PlaylistSortCriteria.SONG_COUNT, R.string.sort_song_count),
    SortOption(PlaylistSortCriteria.DURATION, R.string.sort_duration)
)

/**
 * Immutable UI state for the Playlists Category Screen.
 *
 * @property isLoading Indicates whether playlist data is currently being fetched.
 * @property playlists Full list of playlists returned by the server.
 * @property filteredPlaylists Computed list of playlists after filtering and sorting.
 * @property playlistStatuses Map of playlist IDs to their resolved [DownloadStatus].
 * @property searchQuery Active search query string.
 * @property isSearchActive Whether search input mode is active.
 * @property selectedSort Active sort option.
 * @property isSortAscending Sort direction flag (true for ascending, false for descending).
 * @property isSortSheetOpen Whether the sort bottom sheet is currently open.
 * @property isGridMode Active layout view mode (true for grid, false for list).
 * @property error Localized [UiText] error representation if data fetching failed, or null if successful.
 */
data class PlaylistsUiState(
    val isLoading: Boolean = true,
    val playlists: List<Playlist> = emptyList(),
    val filteredPlaylists: List<Playlist> = emptyList(),
    val playlistStatuses: Map<String, DownloadStatus> = emptyMap(),
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val selectedSort: SortOption<PlaylistSortCriteria> = PLAYLIST_SORT_OPTIONS.first(),
    val isSortAscending: Boolean = true,
    val isSortSheetOpen: Boolean = false,
    val isGridMode: Boolean = false,
    val error: UiText? = null
)
