package com.vsulimov.resonance.ui.screen.albumdetail

/**
 * Represents the possible UI states of the album detail screen.
 */
sealed interface AlbumDetailScreenState {

    /** Initial loading state while album data is being fetched. */
    data object Loading : AlbumDetailScreenState

    /**
     * Album data loaded successfully.
     *
     * @property album The album detail view object ready for display.
     */
    data class Content(val album: AlbumDetailViewObject) : AlbumDetailScreenState

    /**
     * An error occurred while loading album data.
     *
     * @property message Human-readable error description.
     */
    data class Error(val message: String) : AlbumDetailScreenState
}
