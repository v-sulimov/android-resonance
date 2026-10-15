package com.vsulimov.resonance.ui.screens.library.categories.favorites

import com.vsulimov.libsubsonic.data.response.browsing.Album
import com.vsulimov.libsubsonic.data.response.browsing.Artist
import com.vsulimov.libsubsonic.data.response.browsing.Child
import com.vsulimov.resonance.R
import com.vsulimov.resonance.core.ui.UiText
import com.vsulimov.resonance.ui.components.SortOption

/**
 * Filter selection modes for the Favorites Category Screen.
 */
enum class FavoritesFilter {
    ALL,
    SONGS,
    ALBUMS,
    ARTISTS
}

/**
 * Sorting criteria for the Favorites screen.
 */
enum class FavoritesSortCriteria {
    NAME,
    ARTIST
}

/**
 * Available sorting options for the Favorites category.
 */
val FAVORITES_SORT_OPTIONS = listOf(
    SortOption(FavoritesSortCriteria.NAME, R.string.sort_name),
    SortOption(FavoritesSortCriteria.ARTIST, R.string.sort_artist)
)

/**
 * Immutable UI state for the Favorites Category Screen.
 *
 * @property isLoading Indicates whether starred data is currently being fetched.
 * @property selectedFilter Active filter option.
 * @property starredSongs List of starred song tracks.
 * @property starredAlbums List of starred albums.
 * @property starredArtists List of starred artists.
 * @property filteredSongs Filtered & sorted starred songs.
 * @property filteredAlbums Filtered & sorted starred albums.
 * @property filteredArtists Filtered & sorted starred artists.
 * @property searchQuery Active search query string.
 * @property isSearchActive Whether search input mode is active.
 * @property selectedSort Active sort option.
 * @property isSortAscending Sort direction flag (true for ascending, false for descending).
 * @property isSortSheetOpen Whether the sort bottom sheet is currently open.
 * @property error Localized [UiText] error representation if data fetching failed, or null if successful.
 */
data class FavoritesUiState(
    val isLoading: Boolean = true,
    val selectedFilter: FavoritesFilter = FavoritesFilter.ALL,
    val starredSongs: List<Child> = emptyList(),
    val starredAlbums: List<Album> = emptyList(),
    val starredArtists: List<Artist> = emptyList(),
    val filteredSongs: List<Child> = emptyList(),
    val filteredAlbums: List<Album> = emptyList(),
    val filteredArtists: List<Artist> = emptyList(),
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val selectedSort: SortOption<FavoritesSortCriteria> = FAVORITES_SORT_OPTIONS.first(),
    val isSortAscending: Boolean = true,
    val isSortSheetOpen: Boolean = false,
    val error: UiText? = null
)
