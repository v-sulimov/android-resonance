package com.vsulimov.resonance.player.model

/**
 * Represents the player repeat mode state.
 */
enum class RepeatMode {
    /** Repeat mode is disabled. */
    OFF,

    /** Repeat all songs in the current playback queue. */
    ALL,

    /** Repeat the currently active song track. */
    ONE
}
