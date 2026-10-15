package com.vsulimov.resonance.ui.screens.library.categories.artists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vsulimov.libsubsonic.SubsonicClient
import com.vsulimov.libsubsonic.data.response.browsing.ArtistIndex
import com.vsulimov.libsubsonic.data.result.SubsonicResult
import com.vsulimov.resonance.core.ui.UiText
import com.vsulimov.resonance.core.ui.appContainer
import com.vsulimov.resonance.core.utils.ErrorMapper
import com.vsulimov.resonance.data.settings.SettingsRepository
import com.vsulimov.resonance.domain.repository.OfflineRepository
import com.vsulimov.resonance.ui.components.SortOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel responsible for fetching, filtering, and sorting artist index data.
 *
 * @property settingsRepository Repository for checking offline mode toggle.
 * @property offlineRepository Repository for querying offline Room artists.
 * @property getSubsonicClient Provider function for an authenticated [SubsonicClient].
 */
class ArtistsViewModel(
    private val settingsRepository: SettingsRepository,
    private val offlineRepository: OfflineRepository,
    private val getSubsonicClient: () -> SubsonicClient?
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArtistsUiState())

    /** Exposes the immutable UI state for the Artists screen. */
    val uiState: StateFlow<ArtistsUiState> = _uiState.asStateFlow()

    init {
        loadArtists()
    }

    /**
     * Loads the list of artists grouped by alphabetical index from the Subsonic server.
     */
    fun loadArtists() {
        _uiState.update { currentState ->
            val showLoading = currentState.artistIndexes.isEmpty()
            currentState.copy(isLoading = showLoading, error = null)
        }

        if (settingsRepository.isOfflineModeEnabledState.value) {
            viewModelScope.launch {
                val query = _uiState.value.searchQuery
                val offlineFlow = if (query.isBlank()) {
                    offlineRepository.observeAllArtists()
                } else {
                    offlineRepository.searchArtists(query)
                }
                offlineFlow.collect { artists ->
                    val indexes = listOf(ArtistIndex(name = "#", artists = artists))
                    _uiState.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            artistIndexes = indexes,
                            filteredArtistIndexes = applyFilterAndSort(
                                indexes = indexes,
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
                        error = UiText.StringResource(com.vsulimov.resonance.R.string.error_user_not_authenticated)
                    )
                }
                return@launch
            }

            try {
                val result = withContext(Dispatchers.IO) {
                    client.getArtists()
                }

                when (result) {
                    is SubsonicResult.Success -> {
                        val indexes = result.data.artists
                        _uiState.update { currentState ->
                            currentState.copy(
                                isLoading = false,
                                artistIndexes = indexes,
                                filteredArtistIndexes = applyFilterAndSort(
                                    indexes = indexes,
                                    query = currentState.searchQuery,
                                    sortOption = currentState.selectedSort,
                                    isAscending = currentState.isSortAscending
                                ),
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

    /** Updates active search query and recalculates filtered artists. */
    fun setSearchQuery(query: String) {
        _uiState.update { currentState ->
            currentState.copy(
                searchQuery = query,
                filteredArtistIndexes = applyFilterAndSort(
                    indexes = currentState.artistIndexes,
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
                filteredArtistIndexes = applyFilterAndSort(
                    indexes = currentState.artistIndexes,
                    query = newQuery,
                    sortOption = currentState.selectedSort,
                    isAscending = currentState.isSortAscending
                )
            )
        }
    }

    /** Sets selected sorting option. */
    fun setSortOption(option: SortOption<ArtistSortCriteria>) {
        _uiState.update { currentState ->
            currentState.copy(
                selectedSort = option,
                filteredArtistIndexes = applyFilterAndSort(
                    indexes = currentState.artistIndexes,
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
                filteredArtistIndexes = applyFilterAndSort(
                    indexes = currentState.artistIndexes,
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

    private fun applyFilterAndSort(
        indexes: List<ArtistIndex>,
        query: String,
        sortOption: SortOption<ArtistSortCriteria>,
        isAscending: Boolean
    ): List<ArtistIndex> {
        val allArtists = indexes.flatMap { it.artists }

        val filtered = if (query.isBlank()) {
            allArtists
        } else {
            allArtists.filter { it.name.contains(query, ignoreCase = true) }
        }

        val sorted = when (sortOption.id) {
            ArtistSortCriteria.NAME -> filtered.sortedBy { it.name.lowercase() }
            ArtistSortCriteria.ALBUM_COUNT -> filtered.sortedBy { it.albumCount }
        }

        val finalOrdered = if (isAscending) sorted else sorted.reversed()

        if (sortOption.id == ArtistSortCriteria.ALBUM_COUNT) {
            return listOf(ArtistIndex(name = "#", artists = finalOrdered))
        }

        val grouped = finalOrdered.groupBy { artist ->
            val firstChar = artist.name.trim().firstOrNull()?.uppercaseChar() ?: '#'
            if (firstChar in 'A'..'Z') firstChar.toString() else "#"
        }

        return grouped.map { (indexName, artistList) ->
            ArtistIndex(name = indexName, artists = artistList)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val appContainer = this.appContainer
                ArtistsViewModel(
                    settingsRepository = appContainer.settingsRepository,
                    offlineRepository = appContainer.offlineRepository,
                    getSubsonicClient = { appContainer.getAuthenticatedSubsonicClient() }
                )
            }
        }
    }
}
