package com.vsulimov.resonance.ui.screen.artistlist

import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.vsulimov.resonance.domain.usecase.GetArtistListUseCase
import com.vsulimov.resonance.domain.usecase.GetCoverArtUseCase
import com.vsulimov.resonance.ui.mapper.ArtistCardMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * State holder for the Artists list screen.
 *
 * Manages artist loading, screen state lifecycle (loading → content/error),
 * and cover art caching for artist thumbnails.
 *
 * This class is not a ViewModel — it is created by [ArtistListViewModel]
 * which provides the coroutine scope and lifecycle management.
 *
 * @param getArtistListUseCase Fetches all artists from the server.
 * @param getCoverArtUseCase Fetches and caches cover art images.
 * @param coverArtSizePx Cover art size in pixels for thumbnail loading.
 * @param scope Coroutine scope tied to the owning ViewModel's lifecycle.
 */
class ArtistListStateHolder(
    private val getArtistListUseCase: GetArtistListUseCase,
    private val getCoverArtUseCase: GetCoverArtUseCase,
    private val coverArtSizePx: Int,
    private val scope: CoroutineScope
) {

    private val _screenState = MutableStateFlow<ArtistListScreenState>(ArtistListScreenState.Loading)

    /** Current UI state of the Artists list screen. */
    val screenState: StateFlow<ArtistListScreenState> = _screenState.asStateFlow()

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

    init {
        loadJob = scope.launch { loadContent() }
    }

    /**
     * Reloads all artist content. Used for retry after error.
     */
    fun retry() {
        loadJob?.cancel()
        loadJob = scope.launch { loadContent() }
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
     * Fetches all artists and updates the screen state.
     */
    private suspend fun loadContent() {
        _screenState.value = ArtistListScreenState.Loading
        getArtistListUseCase().fold(
            onSuccess = { artists ->
                val cards = ArtistCardMapper.mapList(artists)
                _screenState.value = ArtistListScreenState.Content(cards)
            },
            onFailure = { error ->
                _screenState.value = ArtistListScreenState.Error(error.message ?: "")
            }
        )
    }

    private companion object {
        /** Maximum number of [ImageBitmap] entries kept in [coverArtCache]. */
        const val MAX_COVER_ART_CACHE_SIZE = 100
    }
}
