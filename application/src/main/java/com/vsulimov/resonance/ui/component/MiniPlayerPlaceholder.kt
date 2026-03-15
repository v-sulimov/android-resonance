package com.vsulimov.resonance.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Structural placeholder for the mini player bar.
 *
 * Currently renders nothing — this slot is reserved in the main shell's
 * bottom bar layout so that the mini player can be introduced without
 * restructuring the scaffold when playback is implemented.
 *
 * When active, the mini player will display album art, track name, artist,
 * a favorite toggle, play/pause controls, and a progress indicator.
 *
 * @param modifier Modifier applied to the placeholder (unused while invisible).
 */
@Composable
fun MiniPlayerPlaceholder(modifier: Modifier = Modifier) {
    // Intentionally empty — renders no UI until playback is implemented.
}
