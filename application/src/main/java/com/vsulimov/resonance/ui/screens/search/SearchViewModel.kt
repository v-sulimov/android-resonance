package com.vsulimov.resonance.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vsulimov.libsubsonic.SubsonicClient
import com.vsulimov.libsubsonic.data.result.SubsonicResult
import com.vsulimov.resonance.core.ui.UiText
import com.vsulimov.resonance.core.ui.appContainer
import com.vsulimov.resonance.core.utils.ErrorMapper
import com.vsulimov.resonance.data.settings.SettingsRepository
import com.vsulimov.resonance.domain.repository.OfflineRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val SEARCH_DEBOUNCE_MS = 300L
private const val SEARCH_RESULT_LIMIT = 30

/**
 * ViewModel responsible for managing search input, debounced server calls, and filtering search results.
 *
 * @property settingsRepository Repository for checking offline mode toggle.
 * @property offlineRepository Repository for querying offline Room search results.
 * @property getSubsonicClient Provider function for an authenticated [SubsonicClient].
 */
class SearchViewModel(
    private val settingsRepository: SettingsRepository,
    private val offlineRepository: OfflineRepository,
    private val getSubsonicClient: () -> SubsonicClient?
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())

    /** Exposes immutable UI state for the Search screen. */
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    /** Updates active search query and triggers a debounced search request. */
    fun onSearchQueryChanged(query: String) {
        searchJob?.cancel()

        if (query.isBlank()) {
            _uiState.update {
                it.copy(
                    searchQuery = "",
                    isLoading = false,
                    songs = emptyList(),
                    albums = emptyList(),
                    artists = emptyList(),
                    error = null
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                searchQuery = query,
                isLoading = true,
                error = null
            )
        }

        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            executeSearch(query)
        }
    }

    /** Clears active search query and resets search results. */
    fun clearQuery() {
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                searchQuery = "",
                isLoading = false,
                songs = emptyList(),
                albums = emptyList(),
                artists = emptyList(),
                error = null
            )
        }
    }

    /** Updates active filter selection. */
    fun selectFilter(filter: SearchFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    /** Retries search for current active query. */
    fun retry() {
        val currentQuery = _uiState.value.searchQuery
        if (currentQuery.isNotBlank()) {
            searchJob?.cancel()
            searchJob = viewModelScope.launch {
                executeSearch(currentQuery)
            }
        }
    }

    /**
     * Executes search call to Subsonic server or local Room database if offline mode is enabled.
     *
     * @param query Non-blank search query string.
     */
    private suspend fun executeSearch(query: String) {
        _uiState.update { it.copy(isLoading = true, error = null) }

        if (settingsRepository.isOfflineModeEnabledState.value) {
            val songsFlow = offlineRepository.searchSongs(query)
            val albumsFlow = offlineRepository.searchAlbums(query)
            val artistsFlow = offlineRepository.searchArtists(query)
            combine(songsFlow, albumsFlow, artistsFlow) { songs, albums, artists ->
                Triple(songs, albums, artists)
            }.collect { (songs, albums, artists) ->
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        songs = songs,
                        albums = albums,
                        artists = artists,
                        error = null
                    )
                }
            }
            return
        }

        val client = getSubsonicClient()
        if (client == null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = UiText.StringResource(com.vsulimov.resonance.R.string.error_user_not_authenticated)
                )
            }
            return
        }

        try {
            val result = withContext(Dispatchers.IO) {
                client.search(
                    query = query,
                    artistCount = SEARCH_RESULT_LIMIT,
                    albumCount = SEARCH_RESULT_LIMIT,
                    songCount = SEARCH_RESULT_LIMIT
                )
            }

            when (result) {
                is SubsonicResult.Success -> {
                    val data = result.data
                    _uiState.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            songs = data.songs,
                            albums = data.albums,
                            artists = data.artists,
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
            if (e is kotlinx.coroutines.CancellationException) throw e
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = ErrorMapper.formatError(e)
                )
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val appContainer = this.appContainer
                SearchViewModel(
                    settingsRepository = appContainer.settingsRepository,
                    offlineRepository = appContainer.offlineRepository,
                    getSubsonicClient = { appContainer.getAuthenticatedSubsonicClient() }
                )
            }
        }
    }
}
