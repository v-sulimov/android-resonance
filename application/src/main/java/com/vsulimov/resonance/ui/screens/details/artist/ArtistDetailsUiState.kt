package com.vsulimov.resonance.ui.screens.details.artist

import com.vsulimov.libsubsonic.data.response.browsing.Artist
import com.vsulimov.resonance.core.ui.UiText

/**
 * Immutable UI state for the Artist Details Screen.
 *
 * @property isLoading Indicates whether artist details are currently being loaded.
 * @property artist Detailed artist profile and discography of albums when loaded successfully.
 * @property error Localized [UiText] error representation if data fetching failed, or null.
 */
data class ArtistDetailsUiState(val isLoading: Boolean = true, val artist: Artist? = null, val error: UiText? = null)
