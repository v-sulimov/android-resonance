package com.vsulimov.resonance.ui.screen.main.library

import com.vsulimov.resonance.domain.usecase.GetLibraryCountsUseCase
import com.vsulimov.resonance.ui.mapper.LibraryCategoryMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * State holder for the Library tab screen.
 *
 * Manages the screen state lifecycle (loading → content/error) by
 * fetching library category counts via [GetLibraryCountsUseCase] and
 * mapping them to view objects for the category grid.
 *
 * This class is not a ViewModel — it is created by [LibraryViewModel]
 * which provides the coroutine scope and lifecycle management.
 *
 * @param getLibraryCountsUseCase Fetches all category counts in parallel.
 * @param scope Coroutine scope tied to the owning ViewModel's lifecycle.
 */
class LibraryStateHolder(
    private val getLibraryCountsUseCase: GetLibraryCountsUseCase,
    private val scope: CoroutineScope
) {

    private val _screenState = MutableStateFlow<LibraryScreenState>(LibraryScreenState.Loading)

    /** Current UI state of the Library tab. */
    val screenState: StateFlow<LibraryScreenState> = _screenState.asStateFlow()

    /** Active content loading job, tracked to cancel on retry and prevent concurrent loads. */
    private var loadJob: Job? = null

    init {
        loadJob = scope.launch { loadContent() }
    }

    /**
     * Reloads all library category counts. Used for retry after error.
     *
     * Cancels any in-progress load before starting a new one to prevent
     * concurrent loads from racing to update the screen state.
     */
    fun retry() {
        loadJob?.cancel()
        loadJob = scope.launch { loadContent() }
    }

    /**
     * Fetches all category counts and updates the screen state.
     *
     * Maps successful results to [LibraryScreenState.Content]. If all
     * requests failed, shows [LibraryScreenState.Error] with the message
     * from the first failure.
     */
    private suspend fun loadContent() {
        _screenState.value = LibraryScreenState.Loading
        val counts = getLibraryCountsUseCase()
        val categories = LibraryCategoryMapper.map(counts)

        val allFailed = counts.artistCount.isFailure &&
            counts.albumCount.isFailure &&
            counts.songCount.isFailure &&
            counts.playlistCount.isFailure &&
            counts.genreCount.isFailure &&
            counts.favoriteTrackCount.isFailure

        if (allFailed) {
            val message = counts.artistCount.exceptionOrNull()?.message ?: ""
            _screenState.value = LibraryScreenState.Error(message)
        } else {
            _screenState.value = LibraryScreenState.Content(categories)
        }
    }
}
