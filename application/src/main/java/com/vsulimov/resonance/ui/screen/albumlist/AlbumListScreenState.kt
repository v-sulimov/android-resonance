package com.vsulimov.resonance.ui.screen.albumlist

import com.vsulimov.resonance.ui.screen.main.mix.AlbumCardViewObject

/**
 * UI state for the Albums list screen.
 *
 * Follows the standard Loading → Content / Error pattern used
 * throughout the application, with additional support for
 * incremental page loading.
 */
sealed class AlbumListScreenState {

    /** Initial loading state while the first page is being fetched. */
    data object Loading : AlbumListScreenState()

    /**
     * Content state with album cards to display in a grid.
     *
     * @property albums All loaded album cards across all fetched pages.
     * @property isLoadingMore Whether a subsequent page is currently being fetched.
     * @property hasMore Whether additional pages are available to load.
     */
    data class Content(
        val albums: List<AlbumCardViewObject>,
        val isLoadingMore: Boolean = false,
        val hasMore: Boolean = true
    ) : AlbumListScreenState()

    /**
     * Error state shown when the initial page load fails.
     *
     * @property message Error message from the failed request,
     *   or empty string if no message is available.
     */
    data class Error(val message: String) : AlbumListScreenState()
}
