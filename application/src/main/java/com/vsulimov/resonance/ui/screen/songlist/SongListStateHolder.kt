package com.vsulimov.resonance.ui.screen.songlist

import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.vsulimov.resonance.domain.usecase.GetCoverArtUseCase
import com.vsulimov.resonance.domain.usecase.GetSongListUseCase
import com.vsulimov.resonance.ui.mapper.SongListItemMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * State holder for the Songs list screen.
 *
 * Manages paginated song loading, screen state lifecycle
 * (loading → content/error), and cover art caching for
 * song thumbnails.
 *
 * Cover art is loaded on-demand via composable [LaunchedEffect]s
 * rather than preloaded, matching the approach used by the artists
 * list where the total item count is unbounded.
 *
 * @param getSongListUseCase Fetches paginated song lists.
 * @param getCoverArtUseCase Fetches and caches cover art images.
 * @param coverArtSizePx Cover art size in pixels for thumbnail loading.
 * @param scope Coroutine scope tied to the owning ViewModel's lifecycle.
 */
class SongListStateHolder(
    private val getSongListUseCase: GetSongListUseCase,
    private val getCoverArtUseCase: GetCoverArtUseCase,
    private val coverArtSizePx: Int,
    private val scope: CoroutineScope
) {

    private val _screenState = MutableStateFlow<SongListScreenState>(SongListScreenState.Loading)

    /** Current UI state of the Songs list screen. */
    val screenState: StateFlow<SongListScreenState> = _screenState.asStateFlow()

    /**
     * UI-level LRU cache for loaded cover art images.
     *
     * Stores [ImageBitmap] instances for synchronous access during
     * composition, preventing placeholder flashes when
     * [LazyColumn][androidx.compose.foundation.lazy.LazyColumn]
     * recycles items during scrolling.
     */
    private val coverArtCache = LruCache<String, ImageBitmap>(MAX_COVER_ART_CACHE_SIZE)

    /** Active content loading job, tracked to cancel on retry. */
    private var loadJob: Job? = null

    /** Active pagination job, tracked to prevent concurrent page loads. */
    private var loadMoreJob: Job? = null

    /** Current offset for the next page request. */
    private var currentOffset = 0

    init {
        loadJob = scope.launch { loadFirstPage() }
    }

    /**
     * Reloads all song content from the first page. Used for retry after error.
     */
    fun retry() {
        loadJob?.cancel()
        loadMoreJob?.cancel()
        currentOffset = 0
        loadJob = scope.launch { loadFirstPage() }
    }

    /**
     * Loads the next page of songs if not already loading and more pages exist.
     */
    fun loadMore() {
        val current = _screenState.value
        if (current !is SongListScreenState.Content) return
        if (current.isLoadingMore || !current.hasMore) return

        loadMoreJob?.cancel()
        loadMoreJob = scope.launch { loadNextPage(current) }
    }

    /**
     * Returns a previously loaded cover art image from the UI-level cache.
     */
    fun getCachedCoverArt(id: String): ImageBitmap? = coverArtCache.get(id)

    /**
     * Loads cover art for the given identifier.
     */
    suspend fun loadCoverArt(id: String, sizePx: Int): ImageBitmap? {
        coverArtCache.get(id)?.let { return it }
        val bitmap = getCoverArtUseCase(id, sizePx).getOrNull()?.asImageBitmap()
        if (bitmap != null) coverArtCache.put(id, bitmap)
        return bitmap
    }

    private suspend fun loadFirstPage() {
        _screenState.value = SongListScreenState.Loading
        getSongListUseCase(size = PAGE_SIZE, offset = 0).fold(
            onSuccess = { songs ->
                val items = SongListItemMapper.mapList(songs)
                currentOffset = songs.size
                _screenState.value = SongListScreenState.Content(
                    songs = items,
                    hasMore = songs.size >= PAGE_SIZE
                )
            },
            onFailure = { error ->
                _screenState.value = SongListScreenState.Error(error.message ?: "")
            }
        )
    }

    private suspend fun loadNextPage(current: SongListScreenState.Content) {
        _screenState.value = current.copy(isLoadingMore = true)
        getSongListUseCase(size = PAGE_SIZE, offset = currentOffset).fold(
            onSuccess = { songs ->
                val newItems = SongListItemMapper.mapList(songs)
                currentOffset += songs.size
                _screenState.value = SongListScreenState.Content(
                    songs = current.songs + newItems,
                    isLoadingMore = false,
                    hasMore = songs.size >= PAGE_SIZE
                )
            },
            onFailure = {
                _screenState.value = current.copy(isLoadingMore = false)
            }
        )
    }

    private companion object {
        /** Number of songs to fetch per page. */
        const val PAGE_SIZE = 50

        /** Maximum number of [ImageBitmap] entries kept in [coverArtCache]. */
        const val MAX_COVER_ART_CACHE_SIZE = 100
    }
}
