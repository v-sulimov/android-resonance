package com.vsulimov.resonance.ui.screen.albumdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vsulimov.resonance.ResonanceApplication
import com.vsulimov.resonance.domain.usecase.GetAlbumDetailUseCase
import com.vsulimov.resonance.domain.usecase.GetCoverArtUseCase

/**
 * ViewModel for the album detail screen.
 *
 * Scopes the [AlbumDetailStateHolder] to the ViewModel lifecycle so that
 * album data and loading state survive configuration changes.
 *
 * @param albumId Identifier of the album to display.
 * @param getAlbumDetailUseCase Fetches full album data from the server.
 * @param getCoverArtUseCase Fetches and caches cover art images.
 */
class AlbumDetailViewModel(
    albumId: String,
    getAlbumDetailUseCase: GetAlbumDetailUseCase,
    getCoverArtUseCase: GetCoverArtUseCase
) : ViewModel() {

    /** State holder managing album detail data and cover art loading. */
    val stateHolder = AlbumDetailStateHolder(
        albumId = albumId,
        getAlbumDetailUseCase = getAlbumDetailUseCase,
        getCoverArtUseCase = getCoverArtUseCase,
        scope = viewModelScope
    )

    companion object {

        /** Navigation argument key for the album identifier. */
        const val ALBUM_ID_ARG = "albumId"

        /** Factory that resolves dependencies from [ResonanceApplication.appContainer]. */
        val Factory = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY]
                        as ResonanceApplication
                val container = application.appContainer
                val savedStateHandle: SavedStateHandle = createSavedStateHandle()
                val albumId = requireNotNull(savedStateHandle.get<String>(ALBUM_ID_ARG)) {
                    "Album ID is required"
                }
                AlbumDetailViewModel(
                    albumId = albumId,
                    getAlbumDetailUseCase = container.getAlbumDetailUseCase,
                    getCoverArtUseCase = container.getCoverArtUseCase
                )
            }
        }
    }
}
