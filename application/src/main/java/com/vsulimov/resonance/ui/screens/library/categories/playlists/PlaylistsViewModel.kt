package com.vsulimov.resonance.ui.screens.library.categories.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vsulimov.libsubsonic.SubsonicClient
import com.vsulimov.libsubsonic.data.response.playlists.Playlist
import com.vsulimov.libsubsonic.data.result.SubsonicResult
import com.vsulimov.resonance.R
import com.vsulimov.resonance.core.ui.UiText
import com.vsulimov.resonance.core.ui.appContainer
import com.vsulimov.resonance.core.utils.DownloadUtils
import com.vsulimov.resonance.core.utils.ErrorMapper
import com.vsulimov.resonance.data.db.ResonanceDatabase
import com.vsulimov.resonance.data.settings.SettingsRepository
import com.vsulimov.resonance.data.storage.OfflineStorageManager
import com.vsulimov.resonance.domain.repository.DownloadRepository
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
 * ViewModel responsible for fetching, filtering, and sorting user playlists.
 *
 * @property settingsRepository Repository for checking offline mode toggle.
 * @property offlineRepository Repository for querying offline Room playlists.
 * @property downloadRepository Repository for observing active download tasks.
 * @property database Main Room database for reading playlist song IDs.
 * @property offlineStorageManager Manager for checking audio file presence on disk.
 * @property getSubsonicClient Provider function for an authenticated [SubsonicClient].
 */
class PlaylistsViewModel(
    private val settingsRepository: SettingsRepository,
    private val offlineRepository: OfflineRepository,
    private val downloadRepository: DownloadRepository,
    private val database: ResonanceDatabase,
    private val offlineStorageManager: OfflineStorageManager,
    private val getSubsonicClient: () -> SubsonicClient?
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistsUiState())

    /** Exposes the immutable UI state for the Playlists screen. */
    val uiState: StateFlow<PlaylistsUiState> = _uiState.asStateFlow()

    init {
        observePlaylistStatuses()
        loadPlaylists()
    }

    private fun observePlaylistStatuses() {
        viewModelScope.launch {
            combine(
                offlineRepository.observeAllPlaylists(),
                downloadRepository.observeAllTasks()
            ) { playlists, downloadTasks ->
                withContext(Dispatchers.IO) {
                    playlists.associate { playlist ->
                        val songIds = database.playlistDao().getSongIdsForPlaylist(playlist.id)
                        val status = DownloadUtils.resolveCollectionDownloadStatus(
                            songIds = songIds,
                            downloadTasks = downloadTasks,
                            storageManager = offlineStorageManager,
                            expectedTotalCount = playlist.songCount
                        )
                        playlist.id to status
                    }
                }
            }.collect { statusMap ->
                _uiState.update { it.copy(playlistStatuses = statusMap) }
            }
        }
    }

    /** Loads user playlists from the Subsonic server or local database if offline mode. */
    fun loadPlaylists() {
        _uiState.update { currentState ->
            val showLoading = currentState.playlists.isEmpty()
            currentState.copy(isLoading = showLoading, error = null)
        }

        if (settingsRepository.isOfflineModeEnabledState.value) {
            viewModelScope.launch {
                val query = _uiState.value.searchQuery
                val offlineFlow = if (query.isBlank()) {
                    offlineRepository.observeAllPlaylists()
                } else {
                    offlineRepository.searchPlaylists(query)
                }
                offlineFlow.collect { playlists ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            playlists = playlists,
                            filteredPlaylists = applyFilterAndSort(
                                playlists = playlists,
                                query = currentState.searchQuery,
                                sortOption = currentState.selectedSort,
                                isAscending = currentState.isSortAscending
                            ),
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
                        error = UiText.StringResource(R.string.error_user_not_authenticated)
                    )
                }
                return@launch
            }

            try {
                val result = withContext(Dispatchers.IO) {
                    client.getPlaylists()
                }

                when (result) {
                    is SubsonicResult.Success -> {
                        val playlists = result.data.playlists
                        _uiState.update { currentState ->
                            currentState.copy(
                                isLoading = false,
                                playlists = playlists,
                                filteredPlaylists = applyFilterAndSort(
                                    playlists = playlists,
                                    query = currentState.searchQuery,
                                    sortOption = currentState.selectedSort,
                                    isAscending = currentState.isSortAscending
                                ),
                                error = null
                            )
                        }
                        viewModelScope.launch {
                            offlineRepository.syncPlaylistsGraph(getSubsonicClient)
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

    /** Updates search query and recalculates filtered playlists. */
    fun setSearchQuery(query: String) {
        _uiState.update { currentState ->
            currentState.copy(
                searchQuery = query,
                filteredPlaylists = applyFilterAndSort(
                    playlists = currentState.playlists,
                    query = query,
                    sortOption = currentState.selectedSort,
                    isAscending = currentState.isSortAscending
                )
            )
        }
    }

    /** Toggles search mode visibility. */
    fun toggleSearch() {
        _uiState.update { currentState ->
            val isNowActive = !currentState.isSearchActive
            val newQuery = if (isNowActive) currentState.searchQuery else ""
            currentState.copy(
                isSearchActive = isNowActive,
                searchQuery = newQuery,
                filteredPlaylists = applyFilterAndSort(
                    playlists = currentState.playlists,
                    query = newQuery,
                    sortOption = currentState.selectedSort,
                    isAscending = currentState.isSortAscending
                )
            )
        }
    }

    /** Sets selected sorting option. */
    fun setSortOption(option: SortOption<PlaylistSortCriteria>) {
        _uiState.update { currentState ->
            currentState.copy(
                selectedSort = option,
                filteredPlaylists = applyFilterAndSort(
                    playlists = currentState.playlists,
                    query = currentState.searchQuery,
                    sortOption = option,
                    isAscending = currentState.isSortAscending
                )
            )
        }
    }

    /** Toggles sorting direction. */
    fun toggleSortDirection() {
        _uiState.update { currentState ->
            val newAscending = !currentState.isSortAscending
            currentState.copy(
                isSortAscending = newAscending,
                filteredPlaylists = applyFilterAndSort(
                    playlists = currentState.playlists,
                    query = currentState.searchQuery,
                    sortOption = currentState.selectedSort,
                    isAscending = newAscending
                )
            )
        }
    }

    /** Toggles sort bottom sheet. */
    fun toggleSortSheet() {
        _uiState.update { it.copy(isSortSheetOpen = !it.isSortSheetOpen) }
    }

    /** Toggles between List and Grid display modes. */
    fun toggleViewMode() {
        _uiState.update { it.copy(isGridMode = !it.isGridMode) }
    }

    private fun applyFilterAndSort(
        playlists: List<Playlist>,
        query: String,
        sortOption: SortOption<PlaylistSortCriteria>,
        isAscending: Boolean
    ): List<Playlist> {
        val filtered = if (query.isBlank()) {
            playlists
        } else {
            playlists.filter {
                it.name.contains(query, ignoreCase = true) ||
                    (it.comment?.contains(query, ignoreCase = true) == true)
            }
        }

        val sorted = when (sortOption.id) {
            PlaylistSortCriteria.NAME -> filtered.sortedBy { it.name.lowercase() }
            PlaylistSortCriteria.SONG_COUNT -> filtered.sortedBy { it.songCount }
            PlaylistSortCriteria.DURATION -> filtered.sortedBy { it.duration }
        }

        return if (isAscending) sorted else sorted.reversed()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val appContainer = this.appContainer
                PlaylistsViewModel(
                    settingsRepository = appContainer.settingsRepository,
                    offlineRepository = appContainer.offlineRepository,
                    downloadRepository = appContainer.downloadRepository,
                    database = appContainer.database,
                    offlineStorageManager = appContainer.offlineStorageManager,
                    getSubsonicClient = appContainer::getAuthenticatedSubsonicClient
                )
            }
        }
    }
}
