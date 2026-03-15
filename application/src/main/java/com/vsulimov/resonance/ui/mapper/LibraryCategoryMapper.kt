package com.vsulimov.resonance.ui.mapper

import com.vsulimov.resonance.domain.model.LibraryCounts
import com.vsulimov.resonance.ui.screen.main.library.LibraryCategory
import com.vsulimov.resonance.ui.screen.main.library.LibraryCategoryCardViewObject

/**
 * Maps [LibraryCounts] domain model to a list of [LibraryCategoryCardViewObject]
 * for display in the Library tab grid.
 *
 * Each category is always included in the output list regardless of whether
 * its count succeeded. Failed counts are represented as `null`, allowing the
 * composable to show the category card without a count subtitle.
 */
object LibraryCategoryMapper {

    /**
     * Converts library counts to an ordered list of category card view objects.
     *
     * The order matches the mockup layout: Artists, Albums, Songs,
     * Playlists, Genres, Favorites.
     *
     * @param counts Aggregated library category counts from the use case.
     * @return Ordered list of category cards for the grid.
     */
    fun map(counts: LibraryCounts): List<LibraryCategoryCardViewObject> = listOf(
        LibraryCategoryCardViewObject(
            category = LibraryCategory.ARTISTS,
            count = counts.artistCount.getOrNull()
        ),
        LibraryCategoryCardViewObject(
            category = LibraryCategory.ALBUMS,
            count = counts.albumCount.getOrNull()
        ),
        LibraryCategoryCardViewObject(
            category = LibraryCategory.SONGS,
            count = counts.songCount.getOrNull()
        ),
        LibraryCategoryCardViewObject(
            category = LibraryCategory.PLAYLISTS,
            count = counts.playlistCount.getOrNull()
        ),
        LibraryCategoryCardViewObject(
            category = LibraryCategory.GENRES,
            count = counts.genreCount.getOrNull()
        ),
        LibraryCategoryCardViewObject(
            category = LibraryCategory.FAVORITES,
            count = counts.favoriteTrackCount.getOrNull()
        )
    )
}
