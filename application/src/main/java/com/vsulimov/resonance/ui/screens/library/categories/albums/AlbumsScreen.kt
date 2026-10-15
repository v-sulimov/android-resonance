package com.vsulimov.resonance.ui.screens.library.categories.albums

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsulimov.resonance.R
import com.vsulimov.resonance.core.ui.getAppContainer
import com.vsulimov.resonance.core.utils.DownloadUtils
import com.vsulimov.resonance.core.utils.sanitizeForUi
import com.vsulimov.resonance.data.db.DownloadStatus
import com.vsulimov.resonance.ui.components.CategoryTopBar
import com.vsulimov.resonance.ui.components.EmptyState
import com.vsulimov.resonance.ui.components.ErrorState
import com.vsulimov.resonance.ui.components.LoadingState
import com.vsulimov.resonance.ui.components.MediaGridItem
import com.vsulimov.resonance.ui.components.MediaListItem
import com.vsulimov.resonance.ui.components.ScrollToTopFab
import com.vsulimov.resonance.ui.theme.Dimens
import kotlinx.coroutines.launch

private const val PORTRAIT_COLUMN_COUNT = 2
private const val LANDSCAPE_COLUMN_COUNT = 4
private const val PAGINATION_THRESHOLD = 15
private const val SCROLL_TO_TOP_THRESHOLD = 3

/**
 * Stateful screen composable for the Albums Category view.
 *
 * @param innerPadding Padding values provided by the root Scaffold.
 * @param onBackClick Callback triggered when navigating back.
 * @param modifier The modifier to be applied to the layout.
 * @param viewModel ViewModel holding and managing the screen state.
 */
@Composable
fun AlbumsScreen(
    innerPadding: PaddingValues,
    onBackClick: () -> Unit,
    onAlbumClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: AlbumsViewModel = viewModel(factory = AlbumsViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AlbumsContent(
        uiState = uiState,
        innerPadding = innerPadding,
        onBackClick = onBackClick,
        onRetryClick = viewModel::loadAlbums,
        onLoadMore = viewModel::loadMoreAlbums,
        onSearchQueryChange = viewModel::setSearchQuery,
        onSearchToggle = viewModel::toggleSearch,
        onViewModeToggle = viewModel::toggleViewMode,
        onAlbumClick = onAlbumClick,
        modifier = modifier
    )
}

/**
 * Stateless content composable displaying albums in grid or list view with infinite scroll pagination.
 *
 * @param uiState Current UI state containing albums list and status flags.
 * @param innerPadding Padding values provided by the root Scaffold.
 * @param onBackClick Callback triggered when back navigation is requested.
 * @param onRetryClick Callback triggered when retrying data load after error.
 * @param onLoadMore Callback triggered to fetch the next page of albums.
 * @param onSearchQueryChange Callback triggered on search text input.
 * @param onSearchToggle Callback triggered to toggle search mode.
 * @param onViewModeToggle Callback triggered to toggle between grid and list modes.
 * @param onAlbumClick Callback triggered when an album item is tapped.
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
fun AlbumsContent(
    uiState: AlbumsUiState,
    innerPadding: PaddingValues,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    onLoadMore: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchToggle: () -> Unit,
    onViewModeToggle: () -> Unit,
    onAlbumClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val safeInsets = WindowInsets.safeDrawing.asPaddingValues()
    val leftInset = safeInsets.calculateLeftPadding(LayoutDirection.Ltr)
    val rightInset = safeInsets.calculateRightPadding(LayoutDirection.Ltr)

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val columnsCount = if (isLandscape) LANDSCAPE_COLUMN_COUNT else PORTRAIT_COLUMN_COUNT

    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val appContainer = getAppContainer()
    val downloadTasks by appContainer.downloadRepository.observeAllTasksSimplified().collectAsStateWithLifecycle(
        initialValue = emptyList()
    )

    val isScrollToTopVisible by remember(uiState.isGridMode) {
        derivedStateOf {
            if (uiState.isGridMode) {
                gridState.firstVisibleItemIndex > SCROLL_TO_TOP_THRESHOLD
            } else {
                listState.firstVisibleItemIndex > SCROLL_TO_TOP_THRESHOLD
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding() + Dimens.paddingSmall
                )
        ) {
            CategoryTopBar(
                title = stringResource(R.string.library_category_albums_title),
                onBackClick = onBackClick,
                searchQuery = uiState.searchQuery,
                isSearchActive = uiState.isSearchActive,
                onSearchQueryChange = onSearchQueryChange,
                onSearchToggle = onSearchToggle,
                onViewModeToggle = onViewModeToggle,
                isGridMode = uiState.isGridMode,
                modifier = Modifier.padding(
                    start = leftInset,
                    end = rightInset
                )
            )

            when {
                uiState.isSearchActive && uiState.searchQuery.isBlank() -> {
                    EmptyState(
                        icon = Icons.Default.Search,
                        title = stringResource(R.string.search_prompt_albums_title),
                        description = stringResource(R.string.search_prompt_albums_desc),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding())
                    )
                }

                uiState.isLoading -> {
                    LoadingState(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding())
                    )
                }

                uiState.error != null -> {
                    ErrorState(
                        message = uiState.error.asString(),
                        onRetryClick = onRetryClick,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding())
                    )
                }

                uiState.albums.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Default.Album,
                        title = stringResource(R.string.empty_albums_title),
                        description = stringResource(R.string.empty_albums_desc),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding())
                    )
                }

                uiState.isGridMode -> {
                    val shouldLoadMore by remember {
                        derivedStateOf {
                            val lastVisibleItemIndex = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                            val totalItemsCount = gridState.layoutInfo.totalItemsCount
                            lastVisibleItemIndex >= totalItemsCount - PAGINATION_THRESHOLD && uiState.canPaginate
                        }
                    }

                    LaunchedEffect(shouldLoadMore) {
                        if (shouldLoadMore) {
                            onLoadMore()
                        }
                    }

                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(columnsCount),
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(Dimens.paddingMedium),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.paddingMedium),
                        contentPadding = PaddingValues(
                            bottom = Dimens.paddingLarge + innerPadding.calculateBottomPadding(),
                            start = Dimens.paddingMedium + leftInset,
                            end = Dimens.paddingMedium + rightInset
                        )
                    ) {
                        items(
                            items = uiState.albums,
                            key = { album -> album.id }
                        ) { album ->
                            val albumSongIds = remember(album, downloadTasks) { album.songs.map { it.id } }
                            val albumStatus = DownloadUtils.resolveCollectionDownloadStatus(
                                songIds = albumSongIds,
                                downloadTasks = downloadTasks,
                                storageManager = appContainer.offlineStorageManager,
                                expectedTotalCount = album.songCount ?: album.songs.size
                            )

                            MediaGridItem(
                                title = album.name.sanitizeForUi(stringResource(R.string.unknown_album)),
                                subtitle = album.artist.sanitizeForUi(stringResource(R.string.unknown_artist)),
                                coverArtId = album.coverArt,
                                placeholderIcon = Icons.Default.Album,
                                onClick = { onAlbumClick(album.id) },
                                isDownloaded = albumStatus == DownloadStatus.DOWNLOADED,
                                downloadStatus = albumStatus
                            )
                        }

                        if (uiState.isPaginationLoading) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(Dimens.paddingMedium),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }

                else -> {
                    val shouldLoadMore by remember {
                        derivedStateOf {
                            val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                            val totalItemsCount = listState.layoutInfo.totalItemsCount
                            lastVisibleItemIndex >= totalItemsCount - PAGINATION_THRESHOLD && uiState.canPaginate
                        }
                    }

                    LaunchedEffect(shouldLoadMore) {
                        if (shouldLoadMore) {
                            onLoadMore()
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            bottom = Dimens.paddingLarge + innerPadding.calculateBottomPadding(),
                            start = leftInset,
                            end = rightInset
                        )
                    ) {
                        items(
                            items = uiState.albums,
                            key = { album -> album.id }
                        ) { album ->
                            val albumSongIds = remember(album, downloadTasks) { album.songs.map { it.id } }
                            val albumStatus = DownloadUtils.resolveCollectionDownloadStatus(
                                songIds = albumSongIds,
                                downloadTasks = downloadTasks,
                                storageManager = appContainer.offlineStorageManager,
                                expectedTotalCount = album.songCount ?: album.songs.size
                            )

                            MediaListItem(
                                title = album.name.sanitizeForUi(stringResource(R.string.unknown_album)),
                                subtitle = album.artist.sanitizeForUi(stringResource(R.string.unknown_artist)),
                                coverArtId = album.coverArt,
                                placeholderIcon = Icons.Default.Album,
                                onClick = { onAlbumClick(album.id) },
                                isDownloaded = albumStatus == DownloadStatus.DOWNLOADED,
                                downloadStatus = albumStatus
                            )
                        }

                        if (uiState.isPaginationLoading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(Dimens.paddingMedium),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }
        }

        ScrollToTopFab(
            visible = isScrollToTopVisible,
            onClick = {
                coroutineScope.launch {
                    if (uiState.isGridMode) {
                        gridState.animateScrollToItem(0)
                    } else {
                        listState.animateScrollToItem(0)
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = Dimens.paddingMedium + rightInset,
                    bottom = Dimens.paddingMedium + innerPadding.calculateBottomPadding()
                )
        )
    }
}
