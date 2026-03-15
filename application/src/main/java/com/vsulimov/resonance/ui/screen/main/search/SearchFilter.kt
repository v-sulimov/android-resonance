package com.vsulimov.resonance.ui.screen.main.search

import com.vsulimov.resonance.R

/**
 * Filter categories for search results.
 *
 * Controls which result types are visible in the search results list.
 * Each filter has an associated string resource for its chip label.
 *
 * @param labelResId String resource ID for the filter chip label.
 */
enum class SearchFilter(val labelResId: Int) {
    ALL(R.string.search_filter_all),
    ARTISTS(R.string.search_filter_artists),
    ALBUMS(R.string.search_filter_albums),
    SONGS(R.string.search_filter_songs)
}
