package com.vsulimov.resonance.ui.screen.main.mix

import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Density
import com.vsulimov.resonance.domain.usecase.GetCoverArtUseCase
import com.vsulimov.resonance.domain.usecase.GetMixAlbumsUseCase
import com.vsulimov.resonance.domain.usecase.RefreshRandomPicksUseCase
import com.vsulimov.resonance.ui.component.AlbumArtSize
import com.vsulimov.resonance.ui.mapper.AlbumCardMapper
import com.vsulimov.resonance.ui.mapper.AlbumCarouselMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * State holder for the Mix tab screen.
 *
 * Manages the screen state lifecycle (loading → content/error),
 * handles the "Refresh" action on random picks, and provides
 * cover art loading for album cards.
 *
 * This class is not a ViewModel — it is created by [MixViewModel]
 * which provides the coroutine scope and lifecycle management.
 *
 * @param getMixAlbumsUseCase Fetches all four album carousels in parallel.
 * @param getCoverArtUseCase Fetches and caches cover art images.
 * @param refreshRandomPicksUseCase Fetches a fresh set of random albums.
 * @param density Display density used to convert [AlbumArtSize] to pixels for cover art preloading.
 * @param scope Coroutine scope tied to the owning ViewModel's lifecycle.
 */
class MixStateHolder(
    private val getMixAlbumsUseCase: GetMixAlbumsUseCase,
    private val getCoverArtUseCase: GetCoverArtUseCase,
    private val refreshRandomPicksUseCase: RefreshRandomPicksUseCase,
    density: Density,
    private val scope: CoroutineScope
) {

    /** Cover art size in pixels, computed once from display density. */
    private val coverArtSizePx: Int = with(density) { AlbumArtSize.roundToPx() }

    private val _screenState = MutableStateFlow<MixScreenState>(MixScreenState.Loading)

    /** Current UI state of the Mix tab. */
    val screenState: StateFlow<MixScreenState> = _screenState.asStateFlow()

    /**
     * UI-level LRU cache for loaded cover art images.
     *
     * Unlike the data-layer [LruCache] which caches
     * [Bitmap][android.graphics.Bitmap] objects, this cache stores
     * [ImageBitmap] instances and allows synchronous access during
     * composition. This prevents the placeholder-to-image flash that
     * occurs when [LazyRow][androidx.compose.foundation.lazy.LazyRow]
     * recycles items during scrolling.
     *
     * Capped at [MAX_COVER_ART_CACHE_SIZE] entries to prevent unbounded
     * memory growth after repeated "Refresh" actions on the random picks
     * carousel. Evicted images are re-fetched from the data-layer cache
     * on demand, so the cost of eviction is minimal.
     */
    private val coverArtCache = LruCache<String, ImageBitmap>(MAX_COVER_ART_CACHE_SIZE)

    /** Active content loading job, tracked to cancel on retry and prevent concurrent loads. */
    private var loadJob: Job? = null

    /** Active refresh job, tracked to prevent concurrent refresh requests. */
    private var refreshJob: Job? = null

    init {
        loadJob = scope.launch { loadContent() }
    }

    /**
     * Reloads all Mix tab content. Used for initial load and retry after error.
     *
     * Cancels any in-progress load before starting a new one to prevent
     * concurrent loads from racing to update the screen state.
     */
    fun retry() {
        loadJob?.cancel()
        loadJob = scope.launch { loadContent() }
    }

    /**
     * Fetches a new set of random albums and updates only the
     * "Random picks" carousel in the current content state.
     *
     * Cancels any in-progress refresh before starting a new one.
     * After updating the carousel, preloads cover art for the new
     * albums in the background.
     */
    fun refreshRandomPicks() {
        refreshJob?.cancel()
        refreshJob = scope.launch {
            val result = refreshRandomPicksUseCase()
            result.onSuccess { albums ->
                val currentState = _screenState.value
                if (currentState is MixScreenState.Content) {
                    val cards = AlbumCardMapper.mapList(albums)
                    val updatedCarousels = currentState.carousels.map { carousel ->
                        if (carousel.carouselType == CarouselType.RANDOM_PICKS) {
                            carousel.copy(albums = cards)
                        } else {
                            carousel
                        }
                    }
                    _screenState.value = MixScreenState.Content(updatedCarousels)
                    preloadCoverArt(cards)
                }
            }
        }
    }

    /**
     * Returns a previously loaded cover art image from the UI-level cache.
     *
     * Used during composition to initialize album card state synchronously,
     * avoiding the placeholder flash for already-loaded images when
     * [LazyRow][androidx.compose.foundation.lazy.LazyRow] recycles items.
     *
     * @param id Cover art identifier.
     * @return The cached image, or `null` if not yet loaded.
     */
    fun getCachedCoverArt(id: String): ImageBitmap? = coverArtCache.get(id)

    /**
     * Loads cover art for the given identifier.
     *
     * Called from composable [LaunchedEffect][androidx.compose.runtime.LaunchedEffect]
     * blocks in album cards. Results are cached in the UI-level
     * [coverArtCache] for synchronous access during recomposition,
     * in addition to the data-layer LRU cache.
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
     * Fetches all album lists and updates the screen state.
     *
     * Maps successful results to [MixScreenState.Content]. If no carousels
     * have data and all four requests failed, shows [MixScreenState.Error].
     * If some requests succeeded but returned zero albums (empty library),
     * shows [MixScreenState.Content] with an empty carousel list.
     *
     * After setting the content state, preloads cover art for all albums
     * in the background so images are ready before the user scrolls.
     */
    private suspend fun loadContent() {
        _screenState.value = MixScreenState.Loading
        val mixAlbums = getMixAlbumsUseCase()
        val carousels = AlbumCarouselMapper.map(mixAlbums)

        if (carousels.isEmpty()) {
            val allFailed = mixAlbums.recentlyPlayed.isFailure &&
                mixAlbums.mostPlayed.isFailure &&
                mixAlbums.recentlyAdded.isFailure &&
                mixAlbums.randomPicks.isFailure

            if (allFailed) {
                val message = mixAlbums.recentlyPlayed
                    .exceptionOrNull()?.message ?: ""
                _screenState.value = MixScreenState.Error(message)
            } else {
                _screenState.value = MixScreenState.Content(carousels)
            }
        } else {
            _screenState.value = MixScreenState.Content(carousels)
            preloadCoverArt(carousels.flatMap { it.albums })
        }
    }

    /**
     * Preloads cover art for the given album cards in parallel.
     *
     * Fetches are launched concurrently so all images load as fast as
     * the network allows. Albums without a [coverArtId][AlbumCardViewObject.coverArtId]
     * or already present in [coverArtCache] are skipped.
     *
     * @param albums Album cards whose cover art should be preloaded.
     */
    private suspend fun preloadCoverArt(albums: List<AlbumCardViewObject>) {
        albums
            .mapNotNull { it.coverArtId }
            .filter { coverArtCache.get(it) == null }
            .map { id -> scope.async { loadCoverArt(id, coverArtSizePx) } }
            .awaitAll()
    }

    private companion object {
        /** Maximum number of [ImageBitmap] entries kept in [coverArtCache]. */
        const val MAX_COVER_ART_CACHE_SIZE = 100
    }
}
