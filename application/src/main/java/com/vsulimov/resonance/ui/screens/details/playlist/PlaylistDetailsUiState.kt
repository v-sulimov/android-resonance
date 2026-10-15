package com.vsulimov.resonance.ui.screens.details.playlist

import com.vsulimov.libsubsonic.data.response.playlists.Playlist
import com.vsulimov.resonance.core.ui.UiText

/**
 * Immutable UI state for the Playlist Details Screen.
 *
 * @property isLoading Indicates whether playlist details are currently being loaded.
 * @property playlist Detailed playlist information and track entries when loaded successfully.
 * @property error Localized [UiText] error representation if data fetching failed, or null.
 */
data class PlaylistDetailsUiState(
    val isLoading: Boolean = true,
    val playlist: Playlist? = null,
    val error: UiText? = null
)
