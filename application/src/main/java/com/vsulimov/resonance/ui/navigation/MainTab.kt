package com.vsulimov.resonance.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.ui.graphics.vector.ImageVector
import com.vsulimov.resonance.R

/**
 * Bottom navigation tabs for the main application shell.
 *
 * Each entry defines the string resource for the label displayed beneath
 * the icon and the Material 3 icon vector.
 *
 * @param labelResId String resource ID for the tab label.
 * @param icon Material icon displayed in the navigation bar item.
 */
enum class MainTab(
    @param:StringRes val labelResId: Int,
    val icon: ImageVector
) {

    /** Personalized mix / shuffle tab. */
    MIX(labelResId = R.string.tab_mix, icon = Icons.Outlined.Shuffle),

    /** Library browsing tab (artists, albums, songs, playlists, genres, favorites). */
    LIBRARY(labelResId = R.string.tab_library, icon = Icons.Outlined.LibraryMusic),

    /** Search tab for finding music across the library. */
    SEARCH(labelResId = R.string.tab_search, icon = Icons.Outlined.Search)
}
