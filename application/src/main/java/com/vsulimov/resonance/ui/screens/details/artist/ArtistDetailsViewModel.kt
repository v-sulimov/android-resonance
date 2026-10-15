package com.vsulimov.resonance.ui.screens.details.artist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vsulimov.libsubsonic.SubsonicClient
import com.vsulimov.libsubsonic.data.result.SubsonicResult
import com.vsulimov.resonance.R
import com.vsulimov.resonance.core.ui.UiText
import com.vsulimov.resonance.core.ui.appContainer
import com.vsulimov.resonance.core.utils.ErrorMapper
import com.vsulimov.resonance.data.settings.SettingsRepository
import com.vsulimov.resonance.domain.repository.OfflineRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel responsible for fetching detailed artist profile and discography.
 *
 * @property artistId The unique ID of the artist to fetch.
 * @property settingsRepository Repository for checking offline mode toggle.
 * @property offlineRepository Repository for querying offline Room artist profile.
 * @property getSubsonicClient Provider function for an authenticated [SubsonicClient].
 */
class ArtistDetailsViewModel(
    private val artistId: String,
    private val settingsRepository: SettingsRepository,
    private val offlineRepository: OfflineRepository,
    private val getSubsonicClient: () -> SubsonicClient?
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArtistDetailsUiState())

    /** Exposes the immutable UI state for the Artist Details screen. */
    val uiState: StateFlow<ArtistDetailsUiState> = _uiState.asStateFlow()

    init {
        loadArtistDetails()
    }

    /**
     * Loads artist profile and discography albums from the Subsonic server or local Room database if offline.
     */
    fun loadArtistDetails() {
        _uiState.update { it.copy(isLoading = true, error = null) }

        if (settingsRepository.isOfflineModeEnabledState.value) {
            viewModelScope.launch {
                offlineRepository.observeArtistDetails(artistId).collect { offlineArtist ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            artist = offlineArtist,
                            error = if (offlineArtist ==
                                null
                            ) {
                                UiText.StringResource(R.string.empty_artists_title)
                            } else {
                                null
                            }
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
                        error = UiText.StringResource(R.string.error_user_not_authenticated)
                    )
                }
                return@launch
            }

            try {
                val result = withContext(Dispatchers.IO) {
                    client.getArtist(artistId)
                }

                when (result) {
                    is SubsonicResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                artist = result.data.artist,
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

    companion object {
        /**
         * Returns a [ViewModelProvider.Factory] parameterized with [artistId].
         */
        fun provideFactory(artistId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val appContainer = this.appContainer
                ArtistDetailsViewModel(
                    artistId = artistId,
                    settingsRepository = appContainer.settingsRepository,
                    offlineRepository = appContainer.offlineRepository,
                    getSubsonicClient = { appContainer.getAuthenticatedSubsonicClient() }
                )
            }
        }
    }
}
