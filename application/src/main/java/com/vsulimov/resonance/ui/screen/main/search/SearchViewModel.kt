package com.vsulimov.resonance.ui.screen.main.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vsulimov.resonance.ResonanceApplication
import com.vsulimov.resonance.domain.repository.PreferencesRepository
import com.vsulimov.resonance.domain.usecase.GetCoverArtUseCase
import com.vsulimov.resonance.domain.usecase.SearchUseCase

/**
 * ViewModel for the search screen.
 *
 * Scopes the [SearchStateHolder] to the ViewModel lifecycle so that
 * search state, results, and cover art survive configuration changes.
 *
 * @param searchUseCase Executes search queries against the Subsonic API.
 * @param getCoverArtUseCase Fetches and caches cover art images.
 * @param preferencesRepository Manages search history persistence.
 */
class SearchViewModel(
    searchUseCase: SearchUseCase,
    getCoverArtUseCase: GetCoverArtUseCase,
    preferencesRepository: PreferencesRepository
) : ViewModel() {

    /** State holder managing search query, results, history, and cover art. */
    val stateHolder = SearchStateHolder(
        searchUseCase = searchUseCase,
        getCoverArtUseCase = getCoverArtUseCase,
        preferencesRepository = preferencesRepository,
        scope = viewModelScope
    )

    companion object {

        /** Factory that resolves dependencies from [ResonanceApplication.appContainer]. */
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ResonanceApplication
                val container = app.appContainer
                SearchViewModel(
                    searchUseCase = container.searchUseCase,
                    getCoverArtUseCase = container.getCoverArtUseCase,
                    preferencesRepository = container.preferencesRepository
                )
            }
        }
    }
}
