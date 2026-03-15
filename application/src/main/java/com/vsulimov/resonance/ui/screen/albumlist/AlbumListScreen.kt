package com.vsulimov.resonance.ui.screen.albumlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsulimov.resonance.R
import com.vsulimov.resonance.domain.model.AlbumSortType
import com.vsulimov.resonance.ui.component.AlbumCard
import com.vsulimov.resonance.ui.component.ErrorContent
import kotlinx.coroutines.launch

private val GridHorizontalPadding = 16.dp
private val GridTopPadding = 8.dp
private val GridBottomPadding = 24.dp
private val GridSpacing = 12.dp
private const val LOAD_MORE_THRESHOLD = 6
private const val SCROLL_TO_TOP_THRESHOLD = 0

/**
 * Albums list screen displaying albums in a scrollable two-column grid.
 *
 * Albums are loaded with automatic pagination as the user scrolls near
 * the bottom. The sort type determines the ordering (alphabetical,
 * recently played, most played, or recently added). Each album card
 * shows cover art, title, and artist name, and navigates to the album
 * detail screen on tap.
 *
 * @param sortType Determines the ordering of albums.
 * @param onBack Callback invoked when the back button is tapped.
 * @param onAlbumClick Callback invoked when an album card is tapped,
 *   receiving the album's unique identifier.
 * @param modifier Modifier applied to the root layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumListScreen(
    sortType: AlbumSortType,
    onBack: () -> Unit,
    onAlbumClick: (albumId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: AlbumListViewModel = viewModel(factory = AlbumListViewModel.Factory)
    val stateHolder = viewModel.stateHolder
    val screenState by stateHolder.screenState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    val titleResId = when (sortType) {
        AlbumSortType.ALPHABETICAL -> R.string.album_list_title_alphabetical
        AlbumSortType.RECENTLY_PLAYED -> R.string.album_list_title_recently_played
        AlbumSortType.MOST_PLAYED -> R.string.album_list_title_most_played
        AlbumSortType.RECENTLY_ADDED -> R.string.album_list_title_recently_added
    }

    val showScrollToTop by remember {
        derivedStateOf { gridState.firstVisibleItemIndex > SCROLL_TO_TOP_THRESHOLD }
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(titleResId))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showScrollToTop,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch { gridState.animateScrollToItem(0) }
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowUpward,
                        contentDescription = stringResource(R.string.cd_scroll_to_top)
                    )
                }
            }
        }
    ) { innerPadding ->
        when (val state = screenState) {
            is AlbumListScreenState.Loading -> LoadingContent(
                modifier = Modifier.padding(innerPadding)
            )
            is AlbumListScreenState.Content -> AlbumGridContent(
                state = state,
                stateHolder = stateHolder,
                onAlbumClick = onAlbumClick,
                gridState = gridState,
                innerPadding = innerPadding
            )
            is AlbumListScreenState.Error -> ErrorContent(
                title = stringResource(R.string.album_list_error_load_failed),
                onRetry = stateHolder::retry,
                subtitle = state.message.takeIf { it.isNotEmpty() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}

/**
 * Centered loading indicator shown during initial data fetch.
 */
@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
    }
}

/**
 * Two-column grid of album cards with automatic pagination.
 *
 * Triggers [AlbumListStateHolder.loadMore] when the user scrolls
 * within [LOAD_MORE_THRESHOLD] items of the end.
 */
@Composable
private fun AlbumGridContent(
    state: AlbumListScreenState.Content,
    stateHolder: AlbumListStateHolder,
    onAlbumClick: (albumId: String) -> Unit,
    gridState: LazyGridState,
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleIndex = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = gridState.layoutInfo.totalItemsCount
            lastVisibleIndex >= totalItems - LOAD_MORE_THRESHOLD
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            stateHolder.loadMore()
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = gridState,
        contentPadding = PaddingValues(
            start = GridHorizontalPadding,
            end = GridHorizontalPadding,
            top = GridTopPadding,
            bottom = GridBottomPadding
        ),
        horizontalArrangement = Arrangement.spacedBy(GridSpacing),
        verticalArrangement = Arrangement.spacedBy(GridSpacing),
        modifier = modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        items(
            items = state.albums,
            key = { it.id }
        ) { album ->
            AlbumCard(
                album = album,
                loadCoverArt = stateHolder::loadCoverArt,
                getCachedCoverArt = stateHolder::getCachedCoverArt,
                onClick = { onAlbumClick(album.id) }
            )
        }

        if (state.isLoadingMore) {
            item(
                span = { GridItemSpan(2) }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
