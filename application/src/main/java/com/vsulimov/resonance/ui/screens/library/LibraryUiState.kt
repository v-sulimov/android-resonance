package com.vsulimov.resonance.ui.screens.library

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Identifies the different category navigation targets within the Library screen.
 */
enum class LibraryCategoryId {
    ARTISTS,
    ALBUMS,
    PLAYLISTS,
    FAVORITES,
    DOWNLOADS
}

/**
 * Data model representing a single library category navigation card.
 *
 * @property id The unique identifier for the category.
 * @property titleResId The string resource ID for the category title.
 * @property descriptionResId The string resource ID for the category description.
 * @property icon The Material Vector icon associated with the category.
 * @property route The navigation route associated with this category.
 * @property isAccent Whether this card should be visually highlighted with an accent container color.
 */
data class LibraryCategory(
    val id: LibraryCategoryId,
    @StringRes val titleResId: Int,
    @StringRes val descriptionResId: Int,
    val icon: ImageVector,
    val route: String,
    val isAccent: Boolean = false
)

/**
 * Immutable UI state model for the Library Screen.
 *
 * @property isOfflineMode Whether strict offline mode is active.
 * @property categories The list of library categories to display as navigation cards.
 */
data class LibraryUiState(val isOfflineMode: Boolean = false, val categories: List<LibraryCategory> = emptyList())
