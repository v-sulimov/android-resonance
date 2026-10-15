package com.vsulimov.resonance.ui.screens.library.categories.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vsulimov.libsubsonic.SubsonicClient
import com.vsulimov.libsubsonic.data.response.browsing.Album
import com.vsulimov.libsubsonic.data.response.browsing.Artist
import com.vsulimov.libsubsonic.data.response.browsing.Child
import com.vsulimov.libsubsonic.data.result.SubsonicResult
import com.vsulimov.resonance.core.ui.UiText
import com.vsulimov.resonance.core.ui.appContainer
import com.vsulimov.resonance.core.utils.ErrorMapper
import com.vsulimov.resonance.data.settings.SettingsRepository
import com.vsulimov.resonance.domain.repository.FavoritesRepository
import com.vsulimov.resonance.domain.repository.OfflineRepository
import com.vsulimov.resonance.ui.components.SortOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel responsible for fetching, filtering, and sorting starred items (favorites).
 *
 * @property settingsRepository Repository for checking offline mode toggle.
 * @property offlineRepository Repository for querying offline Room starred items.
 * @property getSubsonicClient Provider function for an authenticated [SubsonicClient].
 * @property favoritesRepository Repository for managing starred status state.
 */
class FavoritesViewModel(
    private val settingsRepository: SettingsRepository,
    private val offlineRepository: OfflineRepository,
    private val getSubsonicClient: () -> SubsonicClient?,
    private val favoritesRepository: FavoritesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())

    /** Exposes the immutable UI state for the Favorites screen. */
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        loadFavorites()
    }

    /** Updates the active filter mode. */
    fun selectFilter(filter: FavoritesFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    /** Loads starred items from the Subsonic server or Room database if offline mode is active. */
    fun loadFavorites() {
        _uiState.update { it.copy(isLoading = true, error = null) }

        if (settingsRepository.isOfflineModeEnabledState.value) {
            viewModelScope.launch {
                val songsFlow = offlineRepository.observeStarredSongs()
                val albumsFlow = offlineRepository.observeStarredAlbums()
                val artistsFlow = offlineRepository.observeStarredArtists()
                combine(songsFlow, albumsFlow, artistsFlow) { songs, albums, artists ->
                    Triple(songs, albums, artists)
                }.collect { (songs, albums, artists) ->
                    _uiState.update { currentState ->
                        val (fSongs, fAlbums, fArtists) = applyFilterAndSort(
                            songs = songs,
                            albums = albums,
                            artists = artists,
                            query = currentState.searchQuery,
                            sortOption = currentState.selectedSort,
                            isAscending = currentState.isSortAscending
                        )
                        currentState.copy(
                            isLoading = false,
                            starredSongs = songs,
                            starredAlbums = albums,
                            starredArtists = artists,
                            filteredSongs = fSongs,
                            filteredAlbums = fAlbums,
                            filteredArtists = fArtists,
                            error = null
                        )
                    }
                }
            }
            return
        }

        viewModelScope.launch {
            val client = getSubsonicClient()
            if (client == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = UiText.StringResource(com.vsulimov.resonance.R.string.error_user_not_authenticated)
                    )
                }
                return@launch
            }

            try {
                val result = withContext(Dispatchers.IO) {
                    client.getStarred()
                }

                when (result) {
                    is SubsonicResult.Success -> {
                        val songs = result.data.songs
                        val albums = result.data.albums
                        val artists = result.data.artists

                        favoritesRepository.loadFavorites()

                        _uiState.update { currentState ->
                            val (fSongs, fAlbums, fArtists) = applyFilterAndSort(
                                songs = songs,
                                albums = albums,
                                artists = artists,
                                query = currentState.searchQuery,
                                sortOption = currentState.selectedSort,
                                isAscending = currentState.isSortAscending
                            )
                            currentState.copy(
                                isLoading = false,
                                starredSongs = songs,
                                starredAlbums = albums,
                                starredArtists = artists,
                                filteredSongs = fSongs,
                                filteredAlbums = fAlbums,
                                filteredArtists = fArtists,
                                error = null
                            )
                        }
                    }

                    is SubsonicResult.Failure -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = ErrorMapper.formatError(result.error)
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = ErrorMapper.formatError(e)
                    )
                }
            }
        }
    }

    /** Updates search query and recalculates filtered items. */
    fun setSearchQuery(query: String) {
        _uiState.update { currentState ->
            val (fSongs, fAlbums, fArtists) = applyFilterAndSort(
                songs = currentState.starredSongs,
                albums = currentState.starredAlbums,
                artists = currentState.starredArtists,
                query = query,
                sortOption = currentState.selectedSort,
                isAscending = currentState.isSortAscending
            )
            currentState.copy(
                searchQuery = query,
                filteredSongs = fSongs,
                filteredAlbums = fAlbums,
                filteredArtists = fArtists
            )
        }
    }

    /** Toggles search mode visibility. */
    fun toggleSearch() {
        _uiState.update { currentState ->
            val isNowActive = !currentState.isSearchActive
            val newQuery = if (isNowActive) currentState.searchQuery else ""
            val (fSongs, fAlbums, fArtists) = applyFilterAndSort(
                songs = currentState.starredSongs,
                albums = currentState.starredAlbums,
                artists = currentState.starredArtists,
                query = newQuery,
                sortOption = currentState.selectedSort,
                isAscending = currentState.isSortAscending
            )
            currentState.copy(
                isSearchActive = isNowActive,
                searchQuery = newQuery,
                filteredSongs = fSongs,
                filteredAlbums = fAlbums,
                filteredArtists = fArtists
            )
        }
    }

    /** Sets selected sorting option. */
    fun setSortOption(option: SortOption<FavoritesSortCriteria>) {
        _uiState.update { currentState ->
            val (fSongs, fAlbums, fArtists) = applyFilterAndSort(
                songs = currentState.starredSongs,
                albums = currentState.starredAlbums,
                artists = currentState.starredArtists,
                query = currentState.searchQuery,
                sortOption = option,
                isAscending = currentState.isSortAscending
            )
            currentState.copy(
                selectedSort = option,
                filteredSongs = fSongs,
                filteredAlbums = fAlbums,
                filteredArtists = fArtists
            )
        }
    }

    /** Toggles sorting direction. */
    fun toggleSortDirection() {
        _uiState.update { currentState ->
            val newAscending = !currentState.isSortAscending
            val (fSongs, fAlbums, fArtists) = applyFilterAndSort(
                songs = currentState.starredSongs,
                albums = currentState.starredAlbums,
                artists = currentState.starredArtists,
                query = currentState.searchQuery,
                sortOption = currentState.selectedSort,
                isAscending = newAscending
            )
            currentState.copy(
                isSortAscending = newAscending,
                filteredSongs = fSongs,
                filteredAlbums = fAlbums,
                filteredArtists = fArtists
            )
        }
    }

    /** Toggles sort bottom sheet. */
    fun toggleSortSheet() {
        _uiState.update { it.copy(isSortSheetOpen = !it.isSortSheetOpen) }
    }

    private fun applyFilterAndSort(
        songs: List<Child>,
        albums: List<Album>,
        artists: List<Artist>,
        query: String,
        sortOption: SortOption<FavoritesSortCriteria>,
        isAscending: Boolean
    ): Triple<List<Child>, List<Album>, List<Artist>> {
        val fSongs = if (query.isBlank()) {
            songs
        } else {
            songs.filter {
                it.title.contains(query, ignoreCase = true) ||
                    (it.artist?.contains(query, ignoreCase = true) == true)
            }
        }

        val fAlbums = if (query.isBlank()) {
            albums
        } else {
            albums.filter {
                it.name.contains(query, ignoreCase = true) ||
                    (it.artist?.contains(query, ignoreCase = true) == true)
            }
        }

        val fArtists = if (query.isBlank()) {
            artists
        } else {
            artists.filter {
                it.name.contains(query, ignoreCase = true)
            }
        }

        val sSongs = when (sortOption.id) {
            FavoritesSortCriteria.NAME -> fSongs.sortedBy { it.title.lowercase() }
            FavoritesSortCriteria.ARTIST -> fSongs.sortedBy { (it.artist ?: "").lowercase() }
        }

        val sAlbums = when (sortOption.id) {
            FavoritesSortCriteria.NAME -> fAlbums.sortedBy { it.name.lowercase() }
            FavoritesSortCriteria.ARTIST -> fAlbums.sortedBy { (it.artist ?: "").lowercase() }
        }

        val sArtists = when (sortOption.id) {
            FavoritesSortCriteria.NAME -> fArtists.sortedBy { it.name.lowercase() }
            FavoritesSortCriteria.ARTIST -> fArtists.sortedBy { it.name.lowercase() }
        }

        return Triple(
            if (isAscending) sSongs else sSongs.reversed(),
            if (isAscending) sAlbums else sAlbums.reversed(),
            if (isAscending) sArtists else sArtists.reversed()
        )
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val appContainer = this.appContainer
                FavoritesViewModel(
                    settingsRepository = appContainer.settingsRepository,
                    offlineRepository = appContainer.offlineRepository,
                    getSubsonicClient = { appContainer.getAuthenticatedSubsonicClient() },
                    favoritesRepository = appContainer.favoritesRepository
                )
            }
        }
    }
}
