package com.vsulimov.resonance.ui.screens.library.categories.artists

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsulimov.resonance.R
import com.vsulimov.resonance.core.ui.getAppContainer
import com.vsulimov.resonance.core.utils.DownloadUtils
import com.vsulimov.resonance.core.utils.sanitizeForUi
import com.vsulimov.resonance.data.db.DownloadStatus
import com.vsulimov.resonance.ui.components.CategoryHeaderActions
import com.vsulimov.resonance.ui.components.CategoryTopBar
import com.vsulimov.resonance.ui.components.EmptyState
import com.vsulimov.resonance.ui.components.ErrorState
import com.vsulimov.resonance.ui.components.LoadingState
import com.vsulimov.resonance.ui.components.MediaListItem
import com.vsulimov.resonance.ui.components.ScrollToTopFab
import com.vsulimov.resonance.ui.components.SortBottomSheet
import com.vsulimov.resonance.ui.theme.Dimens
import kotlinx.coroutines.launch

private const val SCROLL_TO_TOP_THRESHOLD = 3

/**
 * Stateful screen composable for the Artists Category view.
 *
 * @param innerPadding Padding values provided by the root Scaffold.
 * @param onBackClick Callback triggered when navigating back.
 * @param modifier The modifier to be applied to the layout.
 * @param viewModel ViewModel holding and managing the screen state.
 */
@Composable
fun ArtistsScreen(
    innerPadding: PaddingValues,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onArtistClick: (String) -> Unit = {},
    viewModel: ArtistsViewModel = viewModel(factory = ArtistsViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ArtistsContent(
        uiState = uiState,
        innerPadding = innerPadding,
        onBackClick = onBackClick,
        onRetryClick = viewModel::loadArtists,
        onSearchQueryChange = viewModel::setSearchQuery,
        onSearchToggle = viewModel::toggleSearch,
        onSortClick = viewModel::toggleSortSheet,
        onArtistClick = onArtistClick,
        modifier = modifier
    )

    if (uiState.isSortSheetOpen) {
        SortBottomSheet(
            options = ARTIST_SORT_OPTIONS,
            selectedOption = uiState.selectedSort,
            isAscending = uiState.isSortAscending,
            onOptionSelect = viewModel::setSortOption,
            onAscendingToggle = viewModel::toggleSortDirection,
            onDismissRequest = viewModel::toggleSortSheet
        )
    }
}

/**
 * Stateless content composable displaying the list of artists with search and alphabetical index headers.
 *
 * @param uiState Current UI state containing artist index list and status flags.
 * @param innerPadding Padding values provided by the root Scaffold.
 * @param onBackClick Callback triggered when back navigation is requested.
 * @param onRetryClick Callback triggered when retrying data load after error.
 * @param onSearchQueryChange Callback triggered on search text input.
 * @param onSearchToggle Callback triggered to toggle search mode.
 * @param onSortClick Callback triggered to open sort sheet.
 * @param onArtistClick Callback triggered when an artist item is tapped.
 * @param modifier The modifier to be applied to the layout.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArtistsContent(
    uiState: ArtistsUiState,
    innerPadding: PaddingValues,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchToggle: () -> Unit,
    onSortClick: () -> Unit,
    onArtistClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val safeInsets = WindowInsets.safeDrawing.asPaddingValues()
    val leftInset = safeInsets.calculateLeftPadding(LayoutDirection.Ltr)
    val rightInset = safeInsets.calculateRightPadding(LayoutDirection.Ltr)

    val totalArtistsCount = uiState.filteredArtistIndexes.sumOf { it.artists.size }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val appContainer = getAppContainer()
    val downloadTasks by appContainer.downloadRepository.observeAllTasksSimplified().collectAsStateWithLifecycle(
        initialValue = emptyList()
    )

    val isScrollToTopVisible by remember {
        derivedStateOf { listState.firstVisibleItemIndex > SCROLL_TO_TOP_THRESHOLD }
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
                title = stringResource(R.string.library_category_artists_title),
                onBackClick = onBackClick,
                searchQuery = uiState.searchQuery,
                isSearchActive = uiState.isSearchActive,
                onSearchQueryChange = onSearchQueryChange,
                onSearchToggle = onSearchToggle,
                onSortClick = onSortClick,
                modifier = Modifier.padding(
                    start = leftInset,
                    end = rightInset
                )
            )

            when {
                uiState.isSearchActive && uiState.searchQuery.isBlank() -> {
                    EmptyState(
                        icon = Icons.Default.Search,
                        title = stringResource(R.string.search_prompt_artists_title),
                        description = stringResource(R.string.search_prompt_artists_desc),
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

                totalArtistsCount == 0 -> {
                    EmptyState(
                        icon = Icons.Default.Person,
                        title = stringResource(R.string.empty_artists_title),
                        description = stringResource(R.string.empty_artists_desc),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding())
                    )
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
                                itemCount = totalArtistsCount
                            )
                        }

                        uiState.filteredArtistIndexes.forEach { indexGroup ->
                            if (indexGroup.artists.isNotEmpty()) {
                                stickyHeader(key = indexGroup.name) {
                                    Text(
                                        text = indexGroup.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surface)
                                            .padding(
                                                horizontal = Dimens.paddingMedium,
                                                vertical = Dimens.paddingSmall
                                            )
                                    )
                                }

                                items(
                                    items = indexGroup.artists,
                                    key = { artist -> artist.id }
                                ) { artist ->
                                    val subtitle = if (artist.albumCount > 0) {
                                        stringResource(R.string.album_count_format, artist.albumCount)
                                    } else {
                                        null
                                    }

                                    val artistSongs by appContainer.database.songDao().observeSongsByArtistId(
                                        artist.id
                                    ).collectAsStateWithLifecycle(
                                        initialValue = emptyList()
                                    )
                                    val artistStatus = remember(artistSongs, downloadTasks) {
                                        DownloadUtils.resolveCollectionDownloadStatus(
                                            songIds = artistSongs.map { it.id },
                                            downloadTasks = downloadTasks,
                                            storageManager = appContainer.offlineStorageManager
                                        )
                                    }

                                    MediaListItem(
                                        title = artist.name.sanitizeForUi(stringResource(R.string.unknown_artist)),
                                        subtitle = subtitle,
                                        coverArtId = artist.coverArt,
                                        placeholderIcon = Icons.Default.Person,
                                        isCircularArtwork = true,
                                        isDownloaded = artistStatus == DownloadStatus.DOWNLOADED,
                                        onClick = { onArtistClick(artist.id) }
                                    )
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
                    listState.animateScrollToItem(0)
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
