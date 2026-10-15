package com.vsulimov.resonance.ui.screens.library.categories.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vsulimov.libsubsonic.SubsonicClient
import com.vsulimov.libsubsonic.data.response.lists.AlbumListResponse
import com.vsulimov.libsubsonic.data.response.lists.AlbumListType
import com.vsulimov.libsubsonic.data.response.search.SearchResponse
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val ALBUM_PAGE_SIZE = 100
private const val SEARCH_DEBOUNCE_MS = 300L

/**
 * ViewModel responsible for fetching, paginating, server-side searching, and managing albums.
 *
 * @property settingsRepository Repository for checking offline mode state.
 * @property offlineRepository Repository for querying offline Room albums.
 * @property getSubsonicClient Provider function for an authenticated [SubsonicClient].
 */
class AlbumsViewModel(
    private val settingsRepository: SettingsRepository,
    private val offlineRepository: OfflineRepository,
    private val getSubsonicClient: () -> SubsonicClient?
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlbumsUiState())

    /** Exposes the immutable UI state for the Albums screen. */
    val uiState: StateFlow<AlbumsUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadAlbums()
    }

    /** Loads initial page of albums or executes server search if query is non-blank. */
    fun loadAlbums() {
        _uiState.update { currentState ->
            val showLoading = currentState.albums.isEmpty()
            currentState.copy(isLoading = showLoading, isPaginationLoading = false, canPaginate = true, error = null)
        }

        if (settingsRepository.isOfflineModeEnabledState.value) {
            viewModelScope.launch {
                val query = _uiState.value.searchQuery
                val offlineFlow = if (query.isBlank()) {
                    offlineRepository.observeAllAlbums()
                } else {
                    offlineRepository.searchAlbums(query)
                }
                offlineFlow.collect { albums ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            isPaginationLoading = false,
                            canPaginate = false,
                            albums = albums,
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
                val query = _uiState.value.searchQuery
                val result = withContext(Dispatchers.IO) {
                    if (query.isBlank()) {
                        client.getAlbumList(
                            type = AlbumListType.ALPHABETICAL_BY_NAME,
                            size = ALBUM_PAGE_SIZE,
                            offset = 0
                        )
                    } else {
                        client.search(
                            query = query,
                            albumCount = ALBUM_PAGE_SIZE,
                            albumOffset = 0
                        )
                    }
                }

                when (result) {
                    is SubsonicResult.Success -> {
                        val albums = when (val data = result.data) {
                            is AlbumListResponse -> data.albums
                            is SearchResponse -> data.albums
                            else -> emptyList()
                        }

                        _uiState.update { currentState ->
                            currentState.copy(
                                isLoading = false,
                                canPaginate = albums.size >= ALBUM_PAGE_SIZE,
                                albums = albums,
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

    /** Fetches subsequent page of albums (or server search results) and appends to current list. */
    fun loadMoreAlbums() {
        val currentState = _uiState.value
        if (currentState.isLoading || currentState.isPaginationLoading || !currentState.canPaginate) {
            return
        }

        _uiState.update { it.copy(isPaginationLoading = true) }

        viewModelScope.launch {
            val client = getSubsonicClient()
            if (client == null) {
                _uiState.update { it.copy(isPaginationLoading = false) }
                return@launch
            }

            try {
                val currentOffset = _uiState.value.albums.size
                val query = _uiState.value.searchQuery

                val result = withContext(Dispatchers.IO) {
                    if (query.isBlank()) {
                        client.getAlbumList(
                            type = AlbumListType.ALPHABETICAL_BY_NAME,
                            size = ALBUM_PAGE_SIZE,
                            offset = currentOffset
                        )
                    } else {
                        client.search(
                            query = query,
                            albumCount = ALBUM_PAGE_SIZE,
                            albumOffset = currentOffset
                        )
                    }
                }

                when (result) {
                    is SubsonicResult.Success -> {
                        val newPage = when (val data = result.data) {
                            is AlbumListResponse -> data.albums
                            is SearchResponse -> data.albums
                            else -> emptyList()
                        }

                        _uiState.update { state ->
                            val combinedAlbums = state.albums + newPage
                            state.copy(
                                isPaginationLoading = false,
                                canPaginate = newPage.size >= ALBUM_PAGE_SIZE,
                                albums = combinedAlbums
                            )
                        }
                    }

                    is SubsonicResult.Failure -> {
                        _uiState.update { it.copy(isPaginationLoading = false) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isPaginationLoading = false) }
            }
        }
    }

    /** Updates active search query and executes debounced server-side search. */
    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            loadAlbums()
        }
    }

    /** Toggles search input mode. */
    fun toggleSearch() {
        searchJob?.cancel()
        val wasActive = _uiState.value.isSearchActive
        val previousQuery = _uiState.value.searchQuery

        _uiState.update { currentState ->
            currentState.copy(
                isSearchActive = !wasActive,
                searchQuery = if (wasActive) "" else currentState.searchQuery
            )
        }

        if (wasActive && previousQuery.isNotBlank()) {
            loadAlbums()
        }
    }

    /** Toggles between Grid and List display modes. */
    fun toggleViewMode() {
        _uiState.update { it.copy(isGridMode = !it.isGridMode) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val appContainer = this.appContainer
                AlbumsViewModel(
                    settingsRepository = appContainer.settingsRepository,
                    offlineRepository = appContainer.offlineRepository,
                    getSubsonicClient = { appContainer.getAuthenticatedSubsonicClient() }
                )
            }
        }
    }
}
