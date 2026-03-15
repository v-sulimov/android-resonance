package com.vsulimov.resonance.ui.screen.artistlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vsulimov.resonance.ResonanceApplication
import com.vsulimov.resonance.domain.usecase.GetArtistListUseCase
import com.vsulimov.resonance.domain.usecase.GetCoverArtUseCase

/**
 * ViewModel for the Artists list screen.
 *
 * Scopes the [ArtistListStateHolder] to the ViewModel lifecycle so that
 * artist data and cover art survive configuration changes.
 *
 * @param getArtistListUseCase Fetches all artists from the server.
 * @param getCoverArtUseCase Fetches and caches cover art images.
 * @param coverArtSizePx Cover art size in pixels for thumbnail loading.
 */
class ArtistListViewModel(
    getArtistListUseCase: GetArtistListUseCase,
    getCoverArtUseCase: GetCoverArtUseCase,
    coverArtSizePx: Int
) : ViewModel() {

    /** State holder managing artist list data and cover art loading. */
    val stateHolder = ArtistListStateHolder(
        getArtistListUseCase = getArtistListUseCase,
        getCoverArtUseCase = getCoverArtUseCase,
        coverArtSizePx = coverArtSizePx,
        scope = viewModelScope
    )

    companion object {

        /** Factory that resolves dependencies from [ResonanceApplication.appContainer]. */
        val Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as ResonanceApplication
                val container = application.appContainer
                val resources = application.resources
                val coverArtSizePx = (THUMBNAIL_SIZE_DP * resources.displayMetrics.density).toInt()
                ArtistListViewModel(
                    getArtistListUseCase = container.getArtistListUseCase,
                    getCoverArtUseCase = container.getCoverArtUseCase,
                    coverArtSizePx = coverArtSizePx
                )
            }
        }

        /** Thumbnail size in dp for artist cover art. */
        private const val THUMBNAIL_SIZE_DP = 48f
    }
}
