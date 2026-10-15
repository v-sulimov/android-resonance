package com.vsulimov.resonance.player

import com.vsulimov.libsubsonic.data.response.browsing.Child
import com.vsulimov.resonance.player.model.PlaybackState
import kotlinx.coroutines.flow.StateFlow

/**
 * Controller interface for managing audio playback, queue state, and Media3 session interactions.
 */
interface PlayerController {

    /**
     * Observable state flow representing current playback state, active song, and queue.
     */
    val playbackState: StateFlow<PlaybackState>

    /**
     * Plays a single song immediately, setting it as the sole item in the queue.
     *
     * @param song The [Child] song metadata to play.
     */
    fun playSong(song: Child)

    /**
     * Sets the playback queue to [songs] and begins playing at [startIndex].
     *
     * @param songs List of [Child] items representing the queue.
     * @param startIndex 0-based index of the song to start playing.
     */
    fun playQueue(songs: List<Child>, startIndex: Int = 0)

    /**
     * Appends a single [song] to the end of the current playback queue.
     *
     * @param song The [Child] song metadata to append.
     */
    fun addToQueue(song: Child)

    /**
     * Appends a list of [songs] to the end of the current playback queue.
     *
     * @param songs List of [Child] items to append.
     */
    fun addToQueue(songs: List<Child>)

    /**
     * Inserts a single [song] into the queue directly after the currently playing item.
     *
     * @param song The [Child] song metadata to play next.
     */
    fun playNext(song: Child)

    /**
     * Inserts a list of [songs] into the queue directly after the currently playing item.
     *
     * @param songs List of [Child] items to play next.
     */
    fun playNext(songs: List<Child>)

    /**
     * Removes the item at [index] from the current queue.
     *
     * @param index 0-based index of the item to remove.
     */
    fun removeFromQueue(index: Int)

    /**
     * Reorders an item in the queue from [fromIndex] to [toIndex].
     *
     * @param fromIndex Source index.
     * @param toIndex Destination index.
     */
    fun moveQueueItem(fromIndex: Int, toIndex: Int)

    /**
     * Toggles between play and pause states.
     */
    fun togglePlayPause()

    /**
     * Pauses playback.
     */
    fun pause()

    /**
     * Resumes playback.
     */
    fun resume()

    /**
     * Seeks to a specific position in the currently playing track.
     *
     * @param positionMs Target position in milliseconds.
     */
    fun seekTo(positionMs: Long)

    /**
     * Skips directly to the queue item at specified [index].
     *
     * @param index 0-based index of the target track in the queue.
     */
    fun skipToQueueIndex(index: Int)

    /**
     * Skips to the next track in the queue.
     */
    fun skipToNext()

    /**
     * Skips to the previous track in the queue, or restarts current track if elapsed > 3 seconds.
     */
    fun skipToPrevious()

    /**
     * Toggles player shuffle mode.
     */
    fun toggleShuffle()

    /**
     * Cycles through player repeat modes (off -> repeat all -> repeat one -> off).
     */
    fun toggleRepeat()

    /**
     * Clears the current playback queue and stops audio.
     */
    fun clearQueue()

    /**
     * Stops playback and clears the active queue.
     */
    fun stop()

    /**
     * Releases resources and MediaController connection.
     */
    fun release()
}
