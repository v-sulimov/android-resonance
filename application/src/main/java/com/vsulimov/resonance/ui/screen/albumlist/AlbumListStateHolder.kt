package com.vsulimov.resonance.ui.screen.albumlist

import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Density
import com.vsulimov.resonance.domain.model.AlbumSortType
import com.vsulimov.resonance.domain.usecase.GetAlbumListUseCase
import com.vsulimov.resonance.domain.usecase.GetCoverArtUseCase
import com.vsulimov.resonance.ui.component.AlbumArtSize
import com.vsulimov.resonance.ui.mapper.AlbumCardMapper
import com.vsulimov.resonance.ui.screen.main.mix.AlbumCardViewObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * State holder for the Albums list screen.
 *
 * Manages paginated album loading, screen state lifecycle
 * (loading → content/error), and cover art caching for the
 * album grid.
 *
 * This class is not a ViewModel — it is created by [AlbumListViewModel]
 * which provides the coroutine scope and lifecycle management.
 *
 * @param getAlbumListUseCase Fetches paginated album lists.
 * @param getCoverArtUseCase Fetches and caches cover art images.
 * @param sortType Determines the ordering applied by the server.
 * @param density Display density used to convert [AlbumArtSize] to pixels.
 * @param scope Coroutine scope tied to the owning ViewModel's lifecycle.
 */
class AlbumListStateHolder(
    private val getAlbumListUseCase: GetAlbumListUseCase,
    private val getCoverArtUseCase: GetCoverArtUseCase,
    private val sortType: AlbumSortType,
    density: Density,
    private val scope: CoroutineScope
) {

    /** Cover art size in pixels, computed once from display density. */
    private val coverArtSizePx: Int = with(density) { AlbumArtSize.roundToPx() }

    private val _screenState = MutableStateFlow<AlbumListScreenState>(AlbumListScreenState.Loading)

    /** Current UI state of the Albums list screen. */
    val screenState: StateFlow<AlbumListScreenState> = _screenState.asStateFlow()

    /**
     * UI-level LRU cache for loaded cover art images.
     *
     * Stores [ImageBitmap] instances for synchronous access during
     * composition, preventing placeholder flashes when
     * [LazyVerticalGrid][androidx.compose.foundation.lazy.grid.LazyVerticalGrid]
     * recycles items during scrolling.
     */
    private val coverArtCache = LruCache<String, ImageBitmap>(MAX_COVER_ART_CACHE_SIZE)

    /** Active content loading job, tracked to cancel on retry and prevent concurrent loads. */
    private var loadJob: Job? = null

    /** Active pagination job, tracked to prevent concurrent page loads. */
    private var loadMoreJob: Job? = null

    /** Current offset for the next page request. */
    private var currentOffset = 0

    init {
        loadJob = scope.launch { loadFirstPage() }
    }

    /**
     * Reloads all album content from the first page. Used for retry after error.
     *
     * Cancels any in-progress loads before starting a new one.
     */
    fun retry() {
        loadJob?.cancel()
        loadMoreJob?.cancel()
        currentOffset = 0
        loadJob = scope.launch { loadFirstPage() }
    }

    /**
     * Loads the next page of albums if not already loading and more pages exist.
     *
     * Called when the user scrolls near the end of the current list.
     */
    fun loadMore() {
        val current = _screenState.value
        if (current !is AlbumListScreenState.Content) return
        if (current.isLoadingMore || !current.hasMore) return

        loadMoreJob?.cancel()
        loadMoreJob = scope.launch { loadNextPage(current) }
    }

    /**
     * Returns a previously loaded cover art image from the UI-level cache.
     *
     * @param id Cover art identifier.
     * @return The cached image, or `null` if not yet loaded.
     */
    fun getCachedCoverArt(id: String): ImageBitmap? = coverArtCache.get(id)

    /**
     * Loads cover art for the given identifier.
     *
     * @param id Cover art identifier.
     * @param sizePx Requested image dimensions in pixels.
     * @return The decoded image, or `null` if loading failed.
     */
    suspend fun loadCoverArt(id: String, sizePx: Int): ImageBitmap? {
        coverArtCache.get(id)?.let { return it }
        val bitmap = getCoverArtUseCase(id, sizePx).getOrNull()?.asImageBitmap()
        if (bitmap != null) coverArtCache.put(id, bitmap)
        return bitmap
    }

    /**
     * Fetches the first page of albums and updates the screen state.
     */
    private suspend fun loadFirstPage() {
        _screenState.value = AlbumListScreenState.Loading
        getAlbumListUseCase(sortType = sortType, size = PAGE_SIZE, offset = 0).fold(
            onSuccess = { albums ->
                val cards = AlbumCardMapper.mapList(albums)
                currentOffset = albums.size
                _screenState.value = AlbumListScreenState.Content(
                    albums = cards,
                    hasMore = albums.size >= PAGE_SIZE
                )
                preloadCoverArt(cards)
            },
            onFailure = { error ->
                _screenState.value = AlbumListScreenState.Error(error.message ?: "")
            }
        )
    }

    /**
     * Fetches the next page and appends to the existing content.
     */
    private suspend fun loadNextPage(current: AlbumListScreenState.Content) {
        _screenState.value = current.copy(isLoadingMore = true)
        getAlbumListUseCase(sortType = sortType, size = PAGE_SIZE, offset = currentOffset).fold(
            onSuccess = { albums ->
                val newCards = AlbumCardMapper.mapList(albums)
                currentOffset += albums.size
                _screenState.value = AlbumListScreenState.Content(
                    albums = current.albums + newCards,
                    isLoadingMore = false,
                    hasMore = albums.size >= PAGE_SIZE
                )
                preloadCoverArt(newCards)
            },
            onFailure = {
                _screenState.value = current.copy(isLoadingMore = false)
            }
        )
    }

    /**
     * Preloads cover art for the given album cards in parallel.
     *
     * Concurrency is limited by [GetCoverArtUseCase] internally.
     */
    private suspend fun preloadCoverArt(albums: List<AlbumCardViewObject>) {
        albums
            .mapNotNull { it.coverArtId }
            .filter { coverArtCache.get(it) == null }
            .map { id -> scope.async { loadCoverArt(id, coverArtSizePx) } }
            .awaitAll()
    }

    private companion object {
        /** Number of albums to fetch per page. */
        const val PAGE_SIZE = 40

        /** Maximum number of [ImageBitmap] entries kept in [coverArtCache]. */
        const val MAX_COVER_ART_CACHE_SIZE = 100
    }
}
