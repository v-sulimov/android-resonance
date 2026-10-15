package com.vsulimov.resonance.ui.screens.details.playlist

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
import com.vsulimov.resonance.data.db.ResonanceDatabase
import com.vsulimov.resonance.data.db.mapper.toOfflineEntity
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
 * ViewModel responsible for fetching detailed playlist metadata and track entries.
 *
 * @property playlistId The unique ID of the playlist to fetch.
 * @property settingsRepository Repository for checking offline mode toggle.
 * @property offlineRepository Repository for querying offline Room playlist entries.
 * @property database Room database for persisting playlist entries and cross-references.
 * @property getSubsonicClient Provider function for an authenticated [SubsonicClient].
 */
class PlaylistDetailsViewModel(
    private val playlistId: String,
    private val settingsRepository: SettingsRepository,
    private val offlineRepository: OfflineRepository,
    private val database: ResonanceDatabase,
    private val getSubsonicClient: () -> SubsonicClient?
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistDetailsUiState())

    /** Exposes the immutable UI state for the Playlist Details screen. */
    val uiState: StateFlow<PlaylistDetailsUiState> = _uiState.asStateFlow()

    init {
        loadPlaylistDetails()
    }

    /** Loads playlist metadata and entries from the Subsonic server or local database if offline. */
    fun loadPlaylistDetails() {
        _uiState.update { it.copy(isLoading = true, error = null) }

        if (settingsRepository.isOfflineModeEnabledState.value) {
            viewModelScope.launch {
                offlineRepository.observePlaylistDetails(playlistId).collect { playlist ->
                    if (playlist != null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                playlist = playlist,
                                error = null
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = UiText.StringResource(R.string.error_user_not_authenticated)
                            )
                        }
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
                    client.getPlaylist(playlistId)
                }

                when (result) {
                    is SubsonicResult.Success -> {
                        val playlist = result.data.playlist
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                playlist = playlist,
                                error = null
                            )
                        }
                        withContext(Dispatchers.IO) {
                            val songEntities = playlist.entries.map { child ->
                                val existing = database.songDao().getSongById(child.id)
                                child.toOfflineEntity(
                                    localFilePath = existing?.localFilePath,
                                    downloadedAt = existing?.downloadedAt ?: System.currentTimeMillis()
                                )
                            }
                            database.songDao().insertSongs(songEntities)
                            database.playlistDao().updatePlaylistWithSongs(
                                playlist = playlist.toOfflineEntity(),
                                songs = songEntities
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
         * Returns a [ViewModelProvider.Factory] parameterized with [playlistId].
         */
        fun provideFactory(playlistId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val appContainer = this.appContainer
                PlaylistDetailsViewModel(
                    playlistId = playlistId,
                    settingsRepository = appContainer.settingsRepository,
                    offlineRepository = appContainer.offlineRepository,
                    database = appContainer.database,
                    getSubsonicClient = appContainer::getAuthenticatedSubsonicClient
                )
            }
        }
    }
}
