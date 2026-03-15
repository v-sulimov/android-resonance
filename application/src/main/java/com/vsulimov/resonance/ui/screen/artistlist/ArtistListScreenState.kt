package com.vsulimov.resonance.ui.screen.artistlist

/**
 * UI state for the Artists list screen.
 *
 * Follows the standard Loading → Content / Error pattern used
 * throughout the application.
 */
sealed class ArtistListScreenState {

    /** Initial loading state while artists are being fetched. */
    data object Loading : ArtistListScreenState()

    /**
     * Content state with artist cards to display.
     *
     * @property artists All artist cards fetched from the server.
     */
    data class Content(
        val artists: List<ArtistCardViewObject>
    ) : ArtistListScreenState()

    /**
     * Error state shown when the artist list fails to load.
     *
     * @property message Error message from the failed request,
     *   or empty string if no message is available.
     */
    data class Error(val message: String) : ArtistListScreenState()
}
