package com.vsulimov.resonance.ui.screen.main.mix

import androidx.annotation.StringRes

/**
 * Type tag for each carousel section in the Mix tab.
 *
 * Used by the state holder to identify which carousel to update
 * when the user triggers a section-specific action (e.g. refreshing
 * random picks).
 */
enum class CarouselType {
    RECENTLY_PLAYED,
    MOST_PLAYED,
    RECENTLY_ADDED,
    RANDOM_PICKS
}

/**
 * Presentation-layer data class for a horizontal album carousel section.
 *
 * Groups the section header metadata (title, action label) with the
 * list of album cards to display. Mapped from domain [MixAlbums][com.vsulimov.resonance.domain.usecase.MixAlbums]
 * by [AlbumCarouselMapper][com.vsulimov.resonance.ui.mapper.AlbumCarouselMapper].
 *
 * @param titleResId String resource for the section title (e.g. "Recently played").
 * @param actionLabelResId String resource for the header action button (e.g. "See all").
 * @param albums Album cards to display in the horizontal scroll.
 * @param carouselType Identifies this carousel for targeted updates.
 */
data class AlbumCarouselViewObject(
    @param:StringRes val titleResId: Int,
    @param:StringRes val actionLabelResId: Int,
    val albums: List<AlbumCardViewObject>,
    val carouselType: CarouselType
)
