package com.vsulimov.resonance.ui.screen.albumlist

import androidx.compose.ui.unit.Density
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vsulimov.resonance.ResonanceApplication
import com.vsulimov.resonance.domain.model.AlbumSortType
import com.vsulimov.resonance.domain.usecase.GetAlbumListUseCase
import com.vsulimov.resonance.domain.usecase.GetCoverArtUseCase

/**
 * ViewModel for the Albums list screen.
 *
 * Scopes the [AlbumListStateHolder] to the ViewModel lifecycle so that
 * album data, pagination state, and cover art survive configuration changes.
 *
 * @param getAlbumListUseCase Fetches paginated album lists.
 * @param getCoverArtUseCase Fetches and caches cover art images.
 * @param sortType Determines the ordering applied by the server.
 * @param density Display density for converting dp to pixels during cover art preloading.
 */
class AlbumListViewModel(
    getAlbumListUseCase: GetAlbumListUseCase,
    getCoverArtUseCase: GetCoverArtUseCase,
    sortType: AlbumSortType,
    density: Density
) : ViewModel() {

    /** State holder managing album list data, pagination, and cover art loading. */
    val stateHolder = AlbumListStateHolder(
        getAlbumListUseCase = getAlbumListUseCase,
        getCoverArtUseCase = getCoverArtUseCase,
        sortType = sortType,
        density = density,
        scope = viewModelScope
    )

    companion object {

        /** Navigation argument key for the album sort type. */
        const val SORT_TYPE_ARG = "sortType"

        /** Factory that resolves dependencies from [ResonanceApplication.appContainer]. */
        val Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as ResonanceApplication
                val container = application.appContainer
                val resources = application.resources
                val savedStateHandle: SavedStateHandle = createSavedStateHandle()
                val sortTypeName = requireNotNull(savedStateHandle.get<String>(SORT_TYPE_ARG)) {
                    "Sort type is required"
                }
                AlbumListViewModel(
                    getAlbumListUseCase = container.getAlbumListUseCase,
                    getCoverArtUseCase = container.getCoverArtUseCase,
                    sortType = AlbumSortType.valueOf(sortTypeName),
                    density = Density(
                        density = resources.displayMetrics.density,
                        fontScale = resources.configuration.fontScale
                    )
                )
            }
        }
    }
}
