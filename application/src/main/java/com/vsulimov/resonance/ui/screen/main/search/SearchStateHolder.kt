package com.vsulimov.resonance.ui.screen.main.search

import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.vsulimov.resonance.domain.repository.PreferencesRepository
import com.vsulimov.resonance.domain.usecase.GetCoverArtUseCase
import com.vsulimov.resonance.domain.usecase.SearchUseCase
import com.vsulimov.resonance.ui.mapper.SearchResultMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State holder for the search screen.
 *
 * Manages search query input with debounce, search history persistence,
 * filter chip selection, result mapping, and cover art caching.
 *
 * Search is triggered automatically after [DEBOUNCE_DELAY_MS] of inactivity
 * following a query change. The debounce is cancelled when the query changes
 * again, preventing unnecessary API calls during typing.
 *
 * @param searchUseCase Executes search queries against the Subsonic API.
 * @param getCoverArtUseCase Fetches and caches cover art images.
 * @param preferencesRepository Manages search history persistence.
 * @param scope Coroutine scope tied to the owning ViewModel's lifecycle.
 */
class SearchStateHolder(
    private val searchUseCase: SearchUseCase,
    private val getCoverArtUseCase: GetCoverArtUseCase,
    private val preferencesRepository: PreferencesRepository,
    private val scope: CoroutineScope
) {

    private val _uiState = MutableStateFlow(SearchUiState())

    /** Current UI state of the search screen. */
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    /**
     * UI-level LRU cache for loaded cover art images.
     *
     * Shared across all result types (artists, albums, songs) to
     * maximize cache hits and prevent placeholder flashes during
     * filter changes.
     */
    private val coverArtCache = LruCache<String, ImageBitmap>(MAX_COVER_ART_CACHE_SIZE)

    /** Active search job, cancelled on new query input. */
    private var searchJob: Job? = null

    init {
        scope.launch { loadHistory() }
    }

    /**
     * Updates the search query and triggers a debounced search.
     *
     * When the query becomes empty, results are cleared and the
     * history view is shown.
     */
    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()

        if (query.isBlank()) {
            _uiState.update {
                it.copy(
                    resultState = SearchResultState.Idle,
                    filter = SearchFilter.ALL
                )
            }
            return
        }

        searchJob = scope.launch {
            delay(DEBOUNCE_DELAY_MS)
            performSearch(query)
        }
    }

    /**
     * Submits the current query for immediate search and saves to history.
     *
     * Called when the user presses the search action on the keyboard
     * or taps a history item.
     */
    fun onSearch(query: String) {
        if (query.isBlank()) return

        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()
        searchJob = scope.launch {
            preferencesRepository.addSearchHistoryEntry(query)
            loadHistory()
            performSearch(query)
        }
    }

    /**
     * Clears the search query and returns to the history view.
     */
    fun onClearQuery() {
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                query = "",
                resultState = SearchResultState.Idle,
                filter = SearchFilter.ALL
            )
        }
    }

    /**
     * Updates the active result filter.
     */
    fun onFilterChange(filter: SearchFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    /**
     * Removes a single entry from the search history.
     */
    fun onRemoveHistoryEntry(query: String) {
        scope.launch {
            preferencesRepository.removeSearchHistoryEntry(query)
            loadHistory()
        }
    }

    /**
     * Clears all search history entries.
     */
    fun onClearHistory() {
        scope.launch {
            preferencesRepository.clearSearchHistory()
            loadHistory()
        }
    }

    /**
     * Returns a previously loaded cover art image from the UI-level cache.
     */
    fun getCachedCoverArt(id: String): ImageBitmap? = coverArtCache.get(id)

    /**
     * Loads cover art for the given identifier.
     */
    suspend fun loadCoverArt(id: String, sizePx: Int): ImageBitmap? {
        coverArtCache.get(id)?.let { return it }
        val bitmap = getCoverArtUseCase(id, sizePx).getOrNull()?.asImageBitmap()
        if (bitmap != null) coverArtCache.put(id, bitmap)
        return bitmap
    }

    private suspend fun loadHistory() {
        val history = preferencesRepository.getSearchHistory()
        _uiState.update { it.copy(history = history) }
    }

    private suspend fun performSearch(query: String) {
        _uiState.update { it.copy(resultState = SearchResultState.Loading) }

        searchUseCase(query).fold(
            onSuccess = { result ->
                val (artists, albums, songs) = SearchResultMapper.map(result)
                if (artists.isEmpty() && albums.isEmpty() && songs.isEmpty()) {
                    _uiState.update { it.copy(resultState = SearchResultState.Empty) }
                } else {
                    _uiState.update {
                        it.copy(
                            resultState = SearchResultState.Success(
                                artists = artists,
                                albums = albums,
                                songs = songs
                            )
                        )
                    }
                }
            },
            onFailure = { error ->
                _uiState.update {
                    it.copy(resultState = SearchResultState.Error(error.message ?: ""))
                }
            }
        )
    }

    private companion object {
        /** Delay before executing a search after the last query change. */
        const val DEBOUNCE_DELAY_MS = 400L

        /** Maximum number of [ImageBitmap] entries kept in [coverArtCache]. */
        const val MAX_COVER_ART_CACHE_SIZE = 100
    }
}
