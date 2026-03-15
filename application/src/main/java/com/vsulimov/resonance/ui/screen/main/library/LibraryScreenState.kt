package com.vsulimov.resonance.ui.screen.main.library

/**
 * UI state for the Library tab screen.
 *
 * Follows the standard Loading → Content / Error pattern used
 * throughout the application.
 */
sealed class LibraryScreenState {

    /** Initial loading state while category counts are being fetched. */
    data object Loading : LibraryScreenState()

    /**
     * Content state with category cards to display.
     *
     * Individual categories may have `null` counts if their API call
     * failed, but at least one count succeeded.
     *
     * @property categories Ordered list of category cards for the grid.
     */
    data class Content(
        val categories: List<LibraryCategoryCardViewObject>
    ) : LibraryScreenState()

    /**
     * Error state shown when all category count requests failed.
     *
     * @property message Error message from the first failed request,
     *   or empty string if no message is available.
     */
    data class Error(val message: String) : LibraryScreenState()
}
