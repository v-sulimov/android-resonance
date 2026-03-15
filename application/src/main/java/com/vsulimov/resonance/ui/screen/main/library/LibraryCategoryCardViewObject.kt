package com.vsulimov.resonance.ui.screen.main.library

/**
 * Categories displayed on the Library tab grid.
 *
 * Each category maps to a Subsonic API concept and is rendered
 * as a card in a two-column grid. The composable layer uses this
 * enum to resolve the appropriate icon, label, and count plural
 * string resource.
 */
enum class LibraryCategory {
    ARTISTS,
    ALBUMS,
    SONGS,
    PLAYLISTS,
    GENRES,
    FAVORITES
}

/**
 * View object representing a single category card on the Library tab.
 *
 * Carries structured data so that the composable can format counts
 * using localized plural string resources.
 *
 * @property category The library category this card represents.
 * @property count The total item count for this category, or `null`
 *   if the count could not be fetched (API failure).
 */
data class LibraryCategoryCardViewObject(
    val category: LibraryCategory,
    val count: Int?
)
