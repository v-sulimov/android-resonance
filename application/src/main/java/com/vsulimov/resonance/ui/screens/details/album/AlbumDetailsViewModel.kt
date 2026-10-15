package com.vsulimov.resonance.ui.screens.details.album

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
 * ViewModel responsible for fetching detailed album metadata and tracklist.
 *
 * @property albumId The unique ID of the album to fetch.
 * @property settingsRepository Repository for checking offline mode toggle.
 * @property offlineRepository Repository for querying offline Room album details.
 * @property getSubsonicClient Provider function for an authenticated [SubsonicClient].
 */
class AlbumDetailsViewModel(
    private val albumId: String,
    private val settingsRepository: SettingsRepository,
    private val offlineRepository: OfflineRepository,
    private val getSubsonicClient: () -> SubsonicClient?
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlbumDetailsUiState())

    /** Exposes the immutable UI state for the Album Details screen. */
    val uiState: StateFlow<AlbumDetailsUiState> = _uiState.asStateFlow()

    init {
        loadAlbumDetails()
    }

    /**
     * Loads album details and tracks from the Subsonic server or local database if offline mode is active.
     */
    fun loadAlbumDetails() {
        _uiState.update { it.copy(isLoading = true, error = null) }

        if (settingsRepository.isOfflineModeEnabledState.value) {
            viewModelScope.launch {
                offlineRepository.observeAlbumDetails(albumId).collect { offlineAlbum ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            album = offlineAlbum,
                            error = if (offlineAlbum ==
                                null
                            ) {
                                UiText.StringResource(R.string.empty_albums_title)
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
                    client.getAlbum(albumId)
                }

                when (result) {
                    is SubsonicResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                album = result.data.album,
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
         * Returns a [ViewModelProvider.Factory] parameterized with [albumId].
         */
        fun provideFactory(albumId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val appContainer = this.appContainer
                AlbumDetailsViewModel(
                    albumId = albumId,
                    settingsRepository = appContainer.settingsRepository,
                    offlineRepository = appContainer.offlineRepository,
                    getSubsonicClient = { appContainer.getAuthenticatedSubsonicClient() }
                )
            }
        }
    }
}
