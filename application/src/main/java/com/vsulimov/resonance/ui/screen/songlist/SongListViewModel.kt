package com.vsulimov.resonance.ui.screen.songlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vsulimov.resonance.ResonanceApplication
import com.vsulimov.resonance.domain.usecase.GetCoverArtUseCase
import com.vsulimov.resonance.domain.usecase.GetSongListUseCase

/**
 * ViewModel for the Songs list screen.
 *
 * Scopes the [SongListStateHolder] to the ViewModel lifecycle so that
 * song data and cover art survive configuration changes.
 *
 * @param getSongListUseCase Fetches paginated song lists.
 * @param getCoverArtUseCase Fetches and caches cover art images.
 * @param coverArtSizePx Cover art size in pixels for thumbnail loading.
 */
class SongListViewModel(
    getSongListUseCase: GetSongListUseCase,
    getCoverArtUseCase: GetCoverArtUseCase,
    coverArtSizePx: Int
) : ViewModel() {

    /** State holder managing the paginated songs list and cover art. */
    val stateHolder = SongListStateHolder(
        getSongListUseCase = getSongListUseCase,
        getCoverArtUseCase = getCoverArtUseCase,
        coverArtSizePx = coverArtSizePx,
        scope = viewModelScope
    )

    companion object {

        /** Thumbnail size in dp, matching [ListItem] leading content. */
        private const val THUMBNAIL_SIZE_DP = 40f

        /** Factory that resolves dependencies from [ResonanceApplication.appContainer]. */
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ResonanceApplication
                val container = app.appContainer
                val coverArtSizePx = (THUMBNAIL_SIZE_DP * app.resources.displayMetrics.density).toInt()
                SongListViewModel(
                    getSongListUseCase = container.getSongListUseCase,
                    getCoverArtUseCase = container.getCoverArtUseCase,
                    coverArtSizePx = coverArtSizePx
                )
            }
        }
    }
}
