package com.vsulimov.resonance.ui.screen.albumdetail

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.vsulimov.resonance.domain.usecase.GetAlbumDetailUseCase
import com.vsulimov.resonance.domain.usecase.GetCoverArtUseCase
import com.vsulimov.resonance.ui.mapper.AlbumDetailViewObjectMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * State holder for the album detail screen.
 *
 * Manages the screen state lifecycle (loading -> content/error)
 * and provides cover art loading for the album header.
 *
 * This class is not a ViewModel — it is created by [AlbumDetailViewModel]
 * which provides the coroutine scope and lifecycle management.
 *
 * @param albumId Identifier of the album to display.
 * @param getAlbumDetailUseCase Fetches full album data from the server.
 * @param getCoverArtUseCase Fetches and caches cover art images.
 * @param scope Coroutine scope tied to the owning ViewModel's lifecycle.
 */
class AlbumDetailStateHolder(
    private val albumId: String,
    private val getAlbumDetailUseCase: GetAlbumDetailUseCase,
    private val getCoverArtUseCase: GetCoverArtUseCase,
    private val scope: CoroutineScope
) {

    private val _screenState =
        MutableStateFlow<AlbumDetailScreenState>(AlbumDetailScreenState.Loading)

    /** Current UI state of the album detail screen. */
    val screenState: StateFlow<AlbumDetailScreenState> = _screenState.asStateFlow()

    /** Active loading job, tracked to cancel on retry and prevent concurrent loads. */
    private var loadJob: Job? = null

    init {
        loadJob = scope.launch { loadAlbum() }
    }

    /**
     * Reloads album data. Used for retry after error.
     *
     * Cancels any in-progress load before starting a new one to prevent
     * concurrent loads from racing to update the screen state.
     */
    fun retry() {
        loadJob?.cancel()
        loadJob = scope.launch { loadAlbum() }
    }

    /**
     * Loads the album cover art image.
     *
     * Called from a composable [LaunchedEffect][androidx.compose.runtime.LaunchedEffect]
     * in the album header. The underlying repository caches images in memory.
     *
     * @param id Cover art identifier.
     * @param sizePx Requested image dimensions in pixels.
     * @return The decoded image, or `null` if loading failed.
     */
    suspend fun loadCoverArt(id: String, sizePx: Int): ImageBitmap? =
        getCoverArtUseCase(id, sizePx).getOrNull()?.asImageBitmap()

    /**
     * Fetches album details and updates the screen state.
     */
    private suspend fun loadAlbum() {
        _screenState.value = AlbumDetailScreenState.Loading
        getAlbumDetailUseCase(albumId).fold(
            onSuccess = { album ->
                _screenState.value = AlbumDetailScreenState.Content(
                    AlbumDetailViewObjectMapper.map(album)
                )
            },
            onFailure = { error ->
                _screenState.value = AlbumDetailScreenState.Error(
                    error.message ?: ""
                )
            }
        )
    }
}
