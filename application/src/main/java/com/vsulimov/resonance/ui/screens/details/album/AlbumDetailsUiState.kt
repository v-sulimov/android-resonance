package com.vsulimov.resonance.ui.screens.details.album

import com.vsulimov.libsubsonic.data.response.browsing.Album
import com.vsulimov.resonance.core.ui.UiText

/**
 * Immutable UI state for the Album Details Screen.
 *
 * @property isLoading Indicates whether album details are currently being loaded.
 * @property album Detailed album data including song tracklist when loaded successfully.
 * @property error Localized [UiText] error representation if data fetching failed, or null.
 */
data class AlbumDetailsUiState(val isLoading: Boolean = true, val album: Album? = null, val error: UiText? = null)
