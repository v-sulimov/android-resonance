package com.vsulimov.resonance.ui.screen.main.mix

import androidx.compose.ui.unit.Density
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vsulimov.resonance.ResonanceApplication
import com.vsulimov.resonance.domain.usecase.GetCoverArtUseCase
import com.vsulimov.resonance.domain.usecase.GetMixAlbumsUseCase
import com.vsulimov.resonance.domain.usecase.RefreshRandomPicksUseCase

/**
 * ViewModel for the Mix tab screen.
 *
 * Scopes the [MixStateHolder] to the ViewModel lifecycle so that
 * carousel data and loading state survive configuration changes.
 *
 * @param getMixAlbumsUseCase Fetches all four album carousels in parallel.
 * @param getCoverArtUseCase Fetches and caches cover art images.
 * @param refreshRandomPicksUseCase Fetches a fresh set of random albums.
 * @param density Display density for converting dp to pixels during cover art preloading.
 */
class MixViewModel(
    getMixAlbumsUseCase: GetMixAlbumsUseCase,
    getCoverArtUseCase: GetCoverArtUseCase,
    refreshRandomPicksUseCase: RefreshRandomPicksUseCase,
    density: Density
) : ViewModel() {

    /** State holder managing Mix tab carousel data and cover art loading. */
    val stateHolder = MixStateHolder(
        getMixAlbumsUseCase = getMixAlbumsUseCase,
        getCoverArtUseCase = getCoverArtUseCase,
        refreshRandomPicksUseCase = refreshRandomPicksUseCase,
        density = density,
        scope = viewModelScope
    )

    companion object {

        /** Factory that resolves dependencies from [ResonanceApplication.appContainer]. */
        val Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as ResonanceApplication
                val container = application.appContainer
                val resources = application.resources
                MixViewModel(
                    getMixAlbumsUseCase = container.getMixAlbumsUseCase,
                    getCoverArtUseCase = container.getCoverArtUseCase,
                    refreshRandomPicksUseCase = container.refreshRandomPicksUseCase,
                    density = Density(
                        density = resources.displayMetrics.density,
                        fontScale = resources.configuration.fontScale
                    )
                )
            }
        }
    }
}
