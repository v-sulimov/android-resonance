package com.vsulimov.resonance.ui.screens.library.categories.playlists

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.vsulimov.resonance.core.utils.DurationFormatter
import com.vsulimov.resonance.core.utils.sanitizeForUi
import com.vsulimov.resonance.data.db.DownloadStatus
import com.vsulimov.resonance.ui.components.CategoryHeaderActions
import com.vsulimov.resonance.ui.components.CategoryTopBar
import com.vsulimov.resonance.ui.components.EmptyState
import com.vsulimov.resonance.ui.components.ErrorState
import com.vsulimov.resonance.ui.components.LoadingState
import com.vsulimov.resonance.ui.components.MediaGridItem
import com.vsulimov.resonance.ui.components.MediaListItem
import com.vsulimov.resonance.ui.components.ScrollToTopFab
import com.vsulimov.resonance.ui.components.SortBottomSheet
import com.vsulimov.resonance.ui.theme.Dimens
import kotlinx.coroutines.launch

private const val PORTRAIT_COLUMN_COUNT = 2
private const val LANDSCAPE_COLUMN_COUNT = 4
private const val SCROLL_TO_TOP_THRESHOLD = 3

/**
 * Stateful screen composable for the Playlists Category view.
 *
 * @param innerPadding Padding values provided by the root Scaffold.
 * @param onBackClick Callback triggered when navigating back.
 * @param modifier The modifier to be applied to the layout.
 * @param onPlaylistClick Callback triggered when a playlist is tapped.
 * @param viewModel ViewModel holding and managing the screen state.
 */
@Composable
fun PlaylistsScreen(
    innerPadding: PaddingValues,
    onBackClick: () -> Unit,
    onPlaylistClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: PlaylistsViewModel = viewModel(factory = PlaylistsViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PlaylistsContent(
        uiState = uiState,
        innerPadding = innerPadding,
        onBackClick = onBackClick,
        onRetryClick = viewModel::loadPlaylists,
        onSearchQueryChange = viewModel::setSearchQuery,
        onSearchToggle = viewModel::toggleSearch,
        onSortClick = viewModel::toggleSortSheet,
        onViewModeToggle = viewModel::toggleViewMode,
        onPlaylistClick = onPlaylistClick,
        modifier = modifier
    )

    if (uiState.isSortSheetOpen) {
        SortBottomSheet(
            options = PLAYLIST_SORT_OPTIONS,
            selectedOption = uiState.selectedSort,
            isAscending = uiState.isSortAscending,
            onOptionSelect = viewModel::setSortOption,
            onAscendingToggle = viewModel::toggleSortDirection,
            onDismissRequest = viewModel::toggleSortSheet
        )
    }
}

/**
 * Stateless content composable displaying playlists in grid or list view.
 *
 * @param uiState Current UI state containing playlists list and status flags.
 * @param innerPadding Padding values provided by the root Scaffold.
 * @param onBackClick Callback triggered when back navigation is requested.
 * @param onRetryClick Callback triggered when retrying data load after error.
 * @param onSearchQueryChange Callback triggered on search text input.
 * @param onSearchToggle Callback triggered to toggle search mode.
 * @param onSortClick Callback triggered to open sort sheet.
 * @param onViewModeToggle Callback triggered to toggle between grid and list modes.
 * @param onPlaylistClick Callback triggered when a playlist item is tapped.
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
fun PlaylistsContent(
    uiState: PlaylistsUiState,
    innerPadding: PaddingValues,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchToggle: () -> Unit,
    onSortClick: () -> Unit,
    onViewModeToggle: () -> Unit,
    onPlaylistClick: (String) -> Unit,
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
                title = stringResource(R.string.library_category_playlists_title),
                onBackClick = onBackClick,
                searchQuery = uiState.searchQuery,
                isSearchActive = uiState.isSearchActive,
                onSearchQueryChange = onSearchQueryChange,
                onSearchToggle = onSearchToggle,
                onSortClick = onSortClick,
                onViewModeToggle = onViewModeToggle,
                isGridMode = uiState.isGridMode,
                modifier = Modifier.padding(
                    start = leftInset,
                    end = rightInset
                )
            )

            when {
                uiState.isLoading -> {
                    LoadingState(modifier = Modifier.fillMaxSize())
                }

                uiState.error != null -> {
                    ErrorState(
                        message = uiState.error.asString(),
                        onRetryClick = onRetryClick,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                uiState.filteredPlaylists.isEmpty() -> {
                    EmptyState(
                        icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                        title = stringResource(R.string.empty_playlists_title),
                        description = stringResource(R.string.empty_playlists_desc),
                        modifier = Modifier.fillMaxSize()
                    )
                }

                uiState.isGridMode -> {
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
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            CategoryHeaderActions(
                                itemCount = uiState.filteredPlaylists.size
                            )
                        }

                        items(
                            items = uiState.filteredPlaylists,
                            key = { playlist -> playlist.id }
                        ) { playlist ->
                            val subtitle = stringResource(R.string.song_count_format, playlist.songCount)
                            val playlistStatus = uiState.playlistStatuses[playlist.id] ?: DownloadStatus.NOT_DOWNLOADED

                            MediaGridItem(
                                title = playlist.name.sanitizeForUi(stringResource(R.string.unknown_title)),
                                subtitle = subtitle,
                                coverArtId = playlist.coverArt,
                                placeholderIcon = Icons.AutoMirrored.Filled.PlaylistPlay,
                                onClick = { onPlaylistClick(playlist.id) },
                                isDownloaded = playlistStatus == DownloadStatus.DOWNLOADED,
                                downloadStatus = playlistStatus
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            bottom = Dimens.paddingLarge + innerPadding.calculateBottomPadding(),
                            start = leftInset,
                            end = rightInset
                        )
                    ) {
                        item {
                            CategoryHeaderActions(
                                itemCount = uiState.filteredPlaylists.size
                            )
                        }

                        items(
                            items = uiState.filteredPlaylists,
                            key = { playlist -> playlist.id }
                        ) { playlist ->
                            val songsCountText = stringResource(R.string.song_count_format, playlist.songCount)
                            val playlistStatus = uiState.playlistStatuses[playlist.id] ?: DownloadStatus.NOT_DOWNLOADED

                            MediaListItem(
                                title = playlist.name.sanitizeForUi(stringResource(R.string.unknown_title)),
                                subtitle = songsCountText,
                                coverArtId = playlist.coverArt,
                                placeholderIcon = Icons.AutoMirrored.Filled.PlaylistPlay,
                                onClick = { onPlaylistClick(playlist.id) },
                                isDownloaded = playlistStatus == DownloadStatus.DOWNLOADED,
                                downloadStatus = playlistStatus,
                                trailingContent = {
                                    if (playlist.duration > 0) {
                                        Text(
                                            text = DurationFormatter.formatSeconds(playlist.duration),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            )
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
