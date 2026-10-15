package com.vsulimov.resonance.ui.screens.library

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vsulimov.resonance.R
import com.vsulimov.resonance.core.ui.appContainer
import com.vsulimov.resonance.data.settings.SettingsRepository
import com.vsulimov.resonance.ui.navigation.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing the Library Screen state and category navigation data.
 *
 * @property settingsRepository Repository for checking and setting offline mode toggle.
 */
class LibraryViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LibraryUiState(
            categories = listOf(
                LibraryCategory(
                    id = LibraryCategoryId.ARTISTS,
                    titleResId = R.string.library_category_artists_title,
                    descriptionResId = R.string.library_category_artists_desc,
                    icon = Icons.Default.Person,
                    route = Screen.Artists.route
                ),
                LibraryCategory(
                    id = LibraryCategoryId.ALBUMS,
                    titleResId = R.string.library_category_albums_title,
                    descriptionResId = R.string.library_category_albums_desc,
                    icon = Icons.Default.Album,
                    route = Screen.Albums.route
                ),
                LibraryCategory(
                    id = LibraryCategoryId.PLAYLISTS,
                    titleResId = R.string.library_category_playlists_title,
                    descriptionResId = R.string.library_category_playlists_desc,
                    icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                    route = Screen.Playlists.route
                ),
                LibraryCategory(
                    id = LibraryCategoryId.FAVORITES,
                    titleResId = R.string.library_category_favorites_title,
                    descriptionResId = R.string.library_category_favorites_desc,
                    icon = Icons.Default.Favorite,
                    route = Screen.Favorites.route,
                    isAccent = true
                ),
                LibraryCategory(
                    id = LibraryCategoryId.DOWNLOADS,
                    titleResId = R.string.library_category_downloads_title,
                    descriptionResId = R.string.library_category_downloads_desc,
                    icon = Icons.Default.Download,
                    route = Screen.Downloads.route
                )
            )
        )
    )

    /** Exposes the immutable UI state for the Library screen. */
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        observeSettings()
    }

    /**
     * Toggles the strict offline mode setting.
     *
     * @param enabled True to enable offline mode, false otherwise.
     */
    fun toggleOfflineMode(enabled: Boolean) {
        settingsRepository.setOfflineModeEnabled(enabled)
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.isOfflineModeEnabledState.collect { enabled ->
                _uiState.update { it.copy(isOfflineMode = enabled) }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val appContainer = this.appContainer
                LibraryViewModel(
                    settingsRepository = appContainer.settingsRepository
                )
            }
        }
    }
}
