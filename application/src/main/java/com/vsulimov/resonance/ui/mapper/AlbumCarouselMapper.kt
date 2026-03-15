package com.vsulimov.resonance.ui.mapper

import com.vsulimov.resonance.R
import com.vsulimov.resonance.domain.model.AlbumSummary
import com.vsulimov.resonance.domain.usecase.MixAlbums
import com.vsulimov.resonance.ui.screen.main.mix.AlbumCarouselViewObject
import com.vsulimov.resonance.ui.screen.main.mix.CarouselType

/**
 * Maps domain [MixAlbums] to a list of [AlbumCarouselViewObject] for the Mix tab.
 *
 * Each successful, non-empty album list becomes a carousel entry with the
 * appropriate title and action label. Failed or empty results are excluded
 * so the UI only shows carousels with actual content.
 */
object AlbumCarouselMapper {

    /**
     * Converts [MixAlbums] into an ordered list of carousel view objects.
     *
     * The order matches the Mix tab layout: Recently played, Most played,
     * Recently added, Random picks.
     *
     * @param mixAlbums Aggregated album list results from the use case.
     * @return Carousel view objects for each successful, non-empty result.
     */
    fun map(mixAlbums: MixAlbums): List<AlbumCarouselViewObject> {
        val carousels = mutableListOf<AlbumCarouselViewObject>()

        addIfPresent(
            carousels = carousels,
            result = mixAlbums.recentlyPlayed,
            titleResId = R.string.mix_section_recently_played,
            actionLabelResId = R.string.mix_action_see_all,
            carouselType = CarouselType.RECENTLY_PLAYED
        )

        addIfPresent(
            carousels = carousels,
            result = mixAlbums.mostPlayed,
            titleResId = R.string.mix_section_most_played,
            actionLabelResId = R.string.mix_action_see_all,
            carouselType = CarouselType.MOST_PLAYED
        )

        addIfPresent(
            carousels = carousels,
            result = mixAlbums.recentlyAdded,
            titleResId = R.string.mix_section_recently_added,
            actionLabelResId = R.string.mix_action_see_all,
            carouselType = CarouselType.RECENTLY_ADDED
        )

        addIfPresent(
            carousels = carousels,
            result = mixAlbums.randomPicks,
            titleResId = R.string.mix_section_random_picks,
            actionLabelResId = R.string.mix_action_refresh,
            carouselType = CarouselType.RANDOM_PICKS
        )

        return carousels
    }

    /**
     * Adds a carousel to the list if the result is successful and non-empty.
     */
    private fun addIfPresent(
        carousels: MutableList<AlbumCarouselViewObject>,
        result: Result<List<AlbumSummary>>,
        titleResId: Int,
        actionLabelResId: Int,
        carouselType: CarouselType
    ) {
        val albums = result.getOrNull()?.takeIf { it.isNotEmpty() } ?: return
        carousels.add(
            AlbumCarouselViewObject(
                titleResId = titleResId,
                actionLabelResId = actionLabelResId,
                albums = AlbumCardMapper.mapList(albums),
                carouselType = carouselType
            )
        )
    }
}
