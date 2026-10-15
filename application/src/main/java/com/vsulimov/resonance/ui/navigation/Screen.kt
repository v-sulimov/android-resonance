package com.vsulimov.resonance.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.vsulimov.resonance.R

/**
 * Defines the navigation destinations available in the app.
 *
 * @property route The unique route string for the Navigation Component.
 * @property titleResId The string resource ID for the screen title.
 * @property icon The Material Vector icon used in navigation UI components.
 */
sealed class Screen(val route: String, @StringRes val titleResId: Int, val icon: ImageVector) {
    data object Home : Screen("home", R.string.nav_home, Icons.Default.Home)
    data object Library : Screen("library", R.string.nav_library, Icons.Default.LibraryMusic)
    data object Search : Screen("search", R.string.nav_search, Icons.Default.Search)

    data object Albums : Screen("library/albums", R.string.library_category_albums_title, Icons.Default.Album)
    data object Artists : Screen("library/artists", R.string.library_category_artists_title, Icons.Default.Person)
    data object Playlists : Screen(
        "library/playlists",
        R.string.library_category_playlists_title,
        Icons.AutoMirrored.Filled.PlaylistPlay
    )
    data object Favorites : Screen(
        "library/favorites",
        R.string.library_category_favorites_title,
        Icons.Default.Favorite
    )
    data object Downloads : Screen(
        "downloads",
        R.string.downloads_screen_title,
        Icons.Default.Download
    )
    data object ManageDownloads : Screen(
        "downloads/manage",
        R.string.manage_downloads_screen_title,
        Icons.Default.Download
    )
    data object Settings : Screen(
        "settings",
        R.string.settings_screen_title,
        Icons.Default.Settings
    )

    data object AlbumDetails : Screen("album/{albumId}", R.string.album_details_title, Icons.Default.Album) {
        const val ARG_ALBUM_ID = "albumId"
        fun createRoute(albumId: String): String = "album/$albumId"
    }

    data object ArtistDetails : Screen("artist/{artistId}", R.string.artist_details_title, Icons.Default.Person) {
        const val ARG_ARTIST_ID = "artistId"
        fun createRoute(artistId: String): String = "artist/$artistId"
    }

    data object PlaylistDetails : Screen(
        "playlist/{playlistId}",
        R.string.playlist_details_title,
        Icons.AutoMirrored.Filled.PlaylistPlay
    ) {
        const val ARG_PLAYLIST_ID = "playlistId"
        fun createRoute(playlistId: String): String = "playlist/$playlistId"
    }

    companion object {
        val bottomNavItems = listOf(Home, Library, Search)
    }
}
