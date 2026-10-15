package com.vsulimov.resonance.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector
import com.vsulimov.resonance.R

/**
 * Defines the main top-level destinations available in the Bottom Navigation Bar.
 *
 * @property route The unique route string for the Navigation Component.
 * @property titleResId The string resource ID for the screen title.
 * @property icon The Material Vector icon used in the bottom bar.
 */
sealed class Screen(val route: String, @StringRes val titleResId: Int, val icon: ImageVector) {
    object Home : Screen("home", R.string.nav_home, Icons.Default.Home)
    object Library : Screen("library", R.string.nav_library, Icons.Default.LibraryMusic)
    object Search : Screen("search", R.string.nav_search, Icons.Default.Search)

    companion object {
        val bottomNavItems = listOf(Home, Library, Search)
    }
}
