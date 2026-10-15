package com.vsulimov.resonance.player.model

import com.vsulimov.libsubsonic.data.response.browsing.Child

/**
 * Immutable state representing the current audio playback status, active media item, and queue.
 *
 * @property currentSong Currently playing song metadata, or null if the queue is empty.
 * @property isPlaying True if audio is actively playing, false if paused or stopped.
 * @property isBuffering True if the media player is currently buffering audio stream bytes.
 * @property currentPositionMs Current playback progress position in milliseconds.
 * @property durationMs Total duration of the current track in milliseconds.
 * @property queue List of songs in the current playback queue.
 * @property queueIndex 0-based index of the currently playing track in [queue], or -1 if empty.
 * @property shuffleModeEnabled True if queue shuffle mode is active.
 * @property repeatMode Active [RepeatMode] setting (OFF, ALL, or ONE).
 */
data class PlaybackState(
    val currentSong: Child? = null,
    val isPlaying: Boolean = false,
    val playWhenReady: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: List<Child> = emptyList(),
    val queueIndex: Int = -1,
    val shuffleModeEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF
)
