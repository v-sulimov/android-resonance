package com.vsulimov.resonance.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.vsulimov.libsubsonic.SubsonicClient
import com.vsulimov.libsubsonic.data.response.browsing.Child
import com.vsulimov.resonance.data.settings.SettingsRepository
import com.vsulimov.resonance.data.storage.OfflineStorageManager
import com.vsulimov.resonance.player.model.PlaybackState
import com.vsulimov.resonance.player.model.RepeatMode
import com.vsulimov.resonance.player.service.ResonanceMediaService
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val POSITION_UPDATE_INTERVAL_MS = 500L
private const val RESTART_THRESHOLD_MS = 3000L
private const val RESTART_PLAYTIME_THRESHOLD_MS = 10_000L
private const val RESTART_POSITION_THRESHOLD_MS = 2_000L
private const val MAX_SCROBBLE_DURATION_THRESHOLD_MS = 240_000L
private const val MIN_SCROBBLE_DURATION_THRESHOLD_MS = 1_000L
private const val EXTRA_SONG_ID = "extra_song_id"
private const val EXTRA_COVER_ART_ID = "extra_cover_art_id"

/**
 * Default implementation of [PlayerController] backed by a Media3 [MediaController]
 * connected asynchronously to [ResonanceMediaService].
 *
 * @param context Application context for creating session token.
 * @param offlineStorageManager Manager for checking local downloaded audio files.
 * @param settingsRepository Repository for checking offline mode toggle.
 * @param getSubsonicClient Provider function returning an authenticated [SubsonicClient].
 */
class DefaultPlayerController(
    context: Context,
    private val offlineStorageManager: OfflineStorageManager? = null,
    private val settingsRepository: SettingsRepository? = null,
    private val getSubsonicClient: () -> SubsonicClient?
) : PlayerController {

    private val applicationContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _playbackState = MutableStateFlow(PlaybackState())
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    private val childCache = ConcurrentHashMap<String, Child>()
    private var positionUpdateJob: Job? = null

    private var lastTrackId: String? = null
    private var nowPlayingSentForTrackId: String? = null
    private var isCurrentTrackScrobbled: Boolean = false
    private var accumulatedPlayTimeMs: Long = 0L

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.contains(Player.EVENT_TIMELINE_CHANGED)) {
                cleanupCache(player)
            }
            syncStateWithPlayer(player)
        }
    }

    init {
        initializeMediaController()
    }

    override fun playSong(song: Child) {
        playQueue(listOf(song), startIndex = 0)
    }

    override fun playQueue(songs: List<Child>, startIndex: Int) {
        if (songs.isEmpty()) return

        songs.forEach { childCache[it.id] = it }
        val mediaItems = songs.map { createMediaItem(it) }
        val validIndex = startIndex.coerceIn(0, songs.lastIndex)

        val controller = mediaController
        if (controller != null) {
            controller.setMediaItems(mediaItems, validIndex, 0L)
            controller.prepare()
            controller.play()
        }
    }

    override fun addToQueue(song: Child) {
        addToQueue(listOf(song))
    }

    override fun addToQueue(songs: List<Child>) {
        if (songs.isEmpty()) return

        val controller = mediaController
        if (controller == null || controller.mediaItemCount == 0) {
            playQueue(songs, startIndex = 0)
            return
        }

        songs.forEach { childCache[it.id] = it }
        val mediaItems = songs.map { createMediaItem(it) }
        controller.addMediaItems(mediaItems)
    }

    override fun playNext(song: Child) {
        playNext(listOf(song))
    }

    override fun playNext(songs: List<Child>) {
        if (songs.isEmpty()) return

        val controller = mediaController
        if (controller == null || controller.mediaItemCount == 0) {
            playQueue(songs, startIndex = 0)
            return
        }

        songs.forEach { childCache[it.id] = it }
        val insertIndex = (controller.currentMediaItemIndex + 1).coerceIn(0, controller.mediaItemCount)
        val mediaItems = songs.map { createMediaItem(it) }
        controller.addMediaItems(insertIndex, mediaItems)
    }

    override fun removeFromQueue(index: Int) {
        val controller = mediaController ?: return
        if (index !in 0 until controller.mediaItemCount) return
        controller.removeMediaItem(index)
    }

    override fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val controller = mediaController ?: return
        if (fromIndex !in 0 until controller.mediaItemCount || toIndex !in 0 until controller.mediaItemCount) return
        if (fromIndex == toIndex) return
        controller.moveMediaItem(fromIndex, toIndex)
    }

    override fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            if (controller.playbackState == Player.STATE_ENDED) {
                controller.seekTo(0, 0L)
                controller.prepare()
            }
            controller.play()
        }
    }

    override fun pause() {
        mediaController?.pause()
    }

    override fun resume() {
        val controller = mediaController ?: return
        if (controller.playbackState == Player.STATE_ENDED) {
            controller.seekTo(0, 0L)
            controller.prepare()
        }
        controller.play()
    }

    override fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
    }

    override fun skipToQueueIndex(index: Int) {
        val controller = mediaController ?: return
        if (index !in 0 until controller.mediaItemCount) return
        controller.seekTo(index, 0L)
    }

    override fun skipToNext() {
        val controller = mediaController ?: return
        if (controller.hasNextMediaItem()) {
            controller.seekToNextMediaItem()
        }
    }

    override fun skipToPrevious() {
        val controller = mediaController ?: return
        if (controller.currentPosition > RESTART_THRESHOLD_MS) {
            controller.seekTo(0L)
        } else if (controller.hasPreviousMediaItem()) {
            controller.seekToPreviousMediaItem()
        } else {
            controller.seekTo(0L)
        }
    }

    override fun toggleShuffle() {
        val controller = mediaController ?: return
        controller.shuffleModeEnabled = !controller.shuffleModeEnabled
    }

    override fun toggleRepeat() {
        val controller = mediaController ?: return
        val nextMode = when (controller.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        controller.repeatMode = nextMode
    }

    override fun clearQueue() {
        stop()
    }

    override fun stop() {
        resetScrobbleState()
        val controller = mediaController
        if (controller != null) {
            controller.stop()
            controller.clearMediaItems()
        }
        stopPositionUpdates()
        _playbackState.value = PlaybackState()
    }

    override fun release() {
        stopPositionUpdates()
        mediaController?.removeListener(playerListener)
        mediaController = null

        controllerFuture?.let { future ->
            MediaController.releaseFuture(future)
            controllerFuture = null
        }

        scope.cancel()
    }

    /**
     * Connects asynchronously to [ResonanceMediaService] via Media3 [MediaController].
     */
    private fun initializeMediaController() {
        val sessionToken = SessionToken(
            applicationContext,
            ComponentName(applicationContext, ResonanceMediaService::class.java)
        )

        controllerFuture = MediaController.Builder(applicationContext, sessionToken).buildAsync()
        controllerFuture?.addListener(
            {
                try {
                    val controller = controllerFuture?.get()
                    if (controller != null) {
                        mediaController = controller
                        controller.addListener(playerListener)
                        syncStateWithPlayer(controller)
                    }
                } catch (_: Exception) {
                    /* Ignored if media service connection fails during setup */
                }
            },
            MoreExecutors.directExecutor()
        )
    }

    /**
     * Constructs a Media3 [MediaItem] from [Child] song metadata and stream URL or local URI.
     *
     * @param song Target [Child] track item.
     * @return Configured [MediaItem] for playback.
     */
    private fun createMediaItem(song: Child): MediaItem {
        val offlineFile = offlineStorageManager?.getAudioFile(song.id, song.suffix)
        val mediaUri = if (offlineFile != null && offlineFile.exists() && offlineFile.length() > 0) {
            Uri.fromFile(offlineFile)
        } else if (settingsRepository?.isOfflineModeEnabledSync() == true) {
            Uri.EMPTY
        } else {
            val client = getSubsonicClient()
            val streamUrl = client?.streamUrl(id = song.id) ?: ""
            Uri.parse(streamUrl)
        }

        val metadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setAlbumTitle(song.album)
            .setExtras(
                Bundle().apply {
                    putString(EXTRA_SONG_ID, song.id)
                    song.coverArt?.let { putString(EXTRA_COVER_ART_ID, it) }
                }
            )
            .build()

        return MediaItem.Builder()
            .setMediaId(song.id)
            .setUri(mediaUri)
            .setMediaMetadata(metadata)
            .build()
    }

    /**
     * Rebuilds the queue by mapping active Timeline MediaItems back to [Child] instances.
     * Uses memory cache to avoid data loss on async operations or service reconnection.
     */
    private fun rebuildQueue(player: Player): List<Child> {
        val count = player.mediaItemCount
        val newQueue = mutableListOf<Child>()
        for (i in 0 until count) {
            val mediaItem = player.getMediaItemAt(i)
            val mediaId = mediaItem.mediaId
            val child = childCache[mediaId]
            if (child != null) {
                newQueue.add(child)
            } else {
                val title = mediaItem.mediaMetadata.title?.toString() ?: "Unknown Title"
                val artist = mediaItem.mediaMetadata.artist?.toString() ?: "Unknown Artist"
                val album = mediaItem.mediaMetadata.albumTitle?.toString() ?: ""
                val extras = mediaItem.mediaMetadata.extras
                val coverArtId = extras?.getString(EXTRA_COVER_ART_ID)

                val fallbackChild = Child(
                    id = mediaId,
                    parent = null,
                    isDir = false,
                    title = title,
                    album = album,
                    artist = artist,
                    track = null,
                    year = null,
                    genre = null,
                    coverArt = coverArtId,
                    size = null,
                    contentType = null,
                    suffix = null,
                    duration = null,
                    bitRate = null,
                    path = null,
                    albumId = null,
                    artistId = null,
                    type = "music"
                )
                childCache[mediaId] = fallbackChild
                newQueue.add(fallbackChild)
            }
        }
        return newQueue
    }

    /**
     * Removes items from cache that are no longer present in the ExoPlayer timeline.
     */
    private fun cleanupCache(player: Player) {
        val activeIds = (0 until player.mediaItemCount).map { player.getMediaItemAt(it).mediaId }.toSet()
        childCache.keys.retainAll(activeIds)
    }

    /**
     * Synchronizes current [PlaybackState] values with player property states.
     *
     * @param player Active [Player] instance.
     */
    private fun syncStateWithPlayer(player: Player) {
        val queue = rebuildQueue(player)
        val index = player.currentMediaItemIndex
        val currentSong = queue.getOrNull(index)
        val isPlaying = player.isPlaying
        val playWhenReady = player.playWhenReady
        val isBuffering = player.playbackState == Player.STATE_BUFFERING
        val duration = player.duration.coerceAtLeast(0L)
        val position = player.currentPosition.coerceAtLeast(0L)
        val isEnded = player.playbackState == Player.STATE_ENDED

        val repeatMode = when (player.repeatMode) {
            Player.REPEAT_MODE_ALL -> RepeatMode.ALL
            Player.REPEAT_MODE_ONE -> RepeatMode.ONE
            else -> RepeatMode.OFF
        }

        handleScrobbleOnTrackUpdate(currentSong, isPlaying, position)
        checkAndSendScrobbleSubmission(currentSong, duration, isEnded)

        _playbackState.update { state ->
            state.copy(
                currentSong = currentSong,
                isPlaying = isPlaying,
                playWhenReady = playWhenReady,
                isBuffering = isBuffering,
                currentPositionMs = position,
                durationMs = if (duration == C.TIME_UNSET) 0L else duration,
                queue = queue,
                queueIndex = index,
                shuffleModeEnabled = player.shuffleModeEnabled,
                repeatMode = repeatMode
            )
        }

        if (isPlaying) {
            startPositionUpdates()
        } else {
            stopPositionUpdates()
        }
    }

    /**
     * Starts a periodic coroutine polling player position updates.
     */
    private fun startPositionUpdates() {
        if (positionUpdateJob?.isActive == true) return

        positionUpdateJob = scope.launch {
            while (true) {
                delay(POSITION_UPDATE_INTERVAL_MS)
                val controller = mediaController ?: break
                if (!controller.isPlaying) break

                val position = controller.currentPosition.coerceAtLeast(0L)
                val duration = controller.duration.coerceAtLeast(0L)

                accumulatedPlayTimeMs += POSITION_UPDATE_INTERVAL_MS

                val currentQueue = rebuildQueue(controller)
                val currentSong = currentQueue.getOrNull(controller.currentMediaItemIndex)

                checkAndSendScrobbleSubmission(
                    song = currentSong,
                    durationMs = duration,
                    isEnded = controller.playbackState == Player.STATE_ENDED
                )

                _playbackState.update { state ->
                    state.copy(
                        currentPositionMs = position,
                        durationMs = if (duration == C.TIME_UNSET) 0L else duration
                    )
                }
            }
        }
    }

    /**
     * Cancels the active position polling coroutine job.
     */
    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    /**
     * Handles track updates and triggers Now Playing scrobble notification if applicable.
     */
    private fun handleScrobbleOnTrackUpdate(song: Child?, isPlaying: Boolean, positionMs: Long) {
        if (song == null) {
            resetScrobbleState()
            return
        }

        if (song.id != lastTrackId) {
            lastTrackId = song.id
            nowPlayingSentForTrackId = null
            isCurrentTrackScrobbled = false
            accumulatedPlayTimeMs = 0L
        } else if ((accumulatedPlayTimeMs > RESTART_PLAYTIME_THRESHOLD_MS) &&
            (positionMs < RESTART_POSITION_THRESHOLD_MS)
        ) {
            nowPlayingSentForTrackId = null
            isCurrentTrackScrobbled = false
            accumulatedPlayTimeMs = 0L
        }

        if (isPlaying && (nowPlayingSentForTrackId != song.id)) {
            nowPlayingSentForTrackId = song.id
            sendNowPlayingScrobble(song.id)
        }
    }

    /**
     * Checks if current playback duration or state meets scrobbling submission criteria.
     */
    private fun checkAndSendScrobbleSubmission(song: Child?, durationMs: Long, isEnded: Boolean) {
        if ((song == null) || isCurrentTrackScrobbled) return

        val targetScrobbleDurationMs = if (durationMs > 0) {
            (durationMs / 2).coerceAtMost(
                MAX_SCROBBLE_DURATION_THRESHOLD_MS
            ).coerceAtLeast(MIN_SCROBBLE_DURATION_THRESHOLD_MS)
        } else {
            MAX_SCROBBLE_DURATION_THRESHOLD_MS
        }

        if (isEnded || (accumulatedPlayTimeMs >= targetScrobbleDurationMs)) {
            isCurrentTrackScrobbled = true
            sendSubmissionScrobble(song.id)
        }
    }

    /**
     * Sends background Now Playing scrobble notification (`submission = false`).
     */
    private fun sendNowPlayingScrobble(songId: String) {
        if (settingsRepository?.isOfflineModeEnabledSync() == true) return
        val client = getSubsonicClient() ?: return
        scope.launch(Dispatchers.IO) {
            try {
                client.scrobble(
                    ids = listOf(songId),
                    submission = false
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                /* Background network failures do not affect playback */
            }
        }
    }

    /**
     * Sends background track completion scrobble submission (`submission = true`).
     */
    private fun sendSubmissionScrobble(songId: String) {
        if (settingsRepository?.isOfflineModeEnabledSync() == true) return
        val client = getSubsonicClient() ?: return
        val timestamp = System.currentTimeMillis()
        scope.launch(Dispatchers.IO) {
            try {
                client.scrobble(
                    ids = listOf(songId),
                    times = listOf(timestamp),
                    submission = true
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                /* Background network failures do not affect playback */
            }
        }
    }

    /**
     * Resets transient scrobbling state variables for a new playback session.
     */
    private fun resetScrobbleState() {
        lastTrackId = null
        nowPlayingSentForTrackId = null
        isCurrentTrackScrobbled = false
        accumulatedPlayTimeMs = 0L
    }
}
