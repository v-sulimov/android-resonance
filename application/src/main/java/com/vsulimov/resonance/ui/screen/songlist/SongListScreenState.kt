package com.vsulimov.resonance.ui.screen.songlist

/**
 * UI state for the Songs list screen.
 */
sealed class SongListScreenState {

    /** Initial loading state before any data is available. */
    data object Loading : SongListScreenState()

    /**
     * Content state with the current list of songs.
     *
     * @param songs Song items currently loaded.
     * @param isLoadingMore Whether a pagination request is in progress.
     * @param hasMore Whether the server may have additional pages.
     */
    data class Content(
        val songs: List<SongListItemViewObject>,
        val isLoadingMore: Boolean = false,
        val hasMore: Boolean = true
    ) : SongListScreenState()

    /**
     * Error state when content loading fails.
     *
     * @param message Human-readable error description.
     */
    data class Error(val message: String) : SongListScreenState()
}
