package com.vsulimov.resonance.ui.screen.main.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vsulimov.resonance.ResonanceApplication
import com.vsulimov.resonance.domain.usecase.GetLibraryCountsUseCase

/**
 * ViewModel for the Library tab screen.
 *
 * Scopes the [LibraryStateHolder] to the ViewModel lifecycle so that
 * category counts and loading state survive configuration changes.
 *
 * @param getLibraryCountsUseCase Fetches all category counts in parallel.
 */
class LibraryViewModel(
    getLibraryCountsUseCase: GetLibraryCountsUseCase
) : ViewModel() {

    /** State holder managing Library tab category counts and loading state. */
    val stateHolder = LibraryStateHolder(
        getLibraryCountsUseCase = getLibraryCountsUseCase,
        scope = viewModelScope
    )

    companion object {

        /** Factory that resolves dependencies from [ResonanceApplication.appContainer]. */
        val Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as ResonanceApplication
                val container = application.appContainer
                LibraryViewModel(
                    getLibraryCountsUseCase = container.getLibraryCountsUseCase
                )
            }
        }
    }
}
