package com.vsulimov.resonance.ui.screen.main.mix

/**
 * Represents the UI state of the Mix tab screen.
 *
 * The screen transitions through these states:
 * 1. [Loading] — initial data fetch in progress.
 * 2. [Content] — at least one carousel has data to display.
 * 3. [Error] — all carousel requests failed.
 */
sealed class MixScreenState {

    /** Album data is being fetched from the server. */
    data object Loading : MixScreenState()

    /**
     * Album carousels are ready for display.
     *
     * May contain zero carousels if the library is empty — in that case
     * only the hero card is shown.
     *
     * @param carousels Ordered list of carousel sections to render.
     */
    data class Content(
        val carousels: List<AlbumCarouselViewObject>
    ) : MixScreenState()

    /**
     * All carousel requests failed.
     *
     * @param message User-facing error description.
     */
    data class Error(
        val message: String
    ) : MixScreenState()
}
