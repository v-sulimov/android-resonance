package com.vsulimov.resonance.ui.screen.songlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsulimov.resonance.R
import com.vsulimov.resonance.ui.component.CoverArtPlaceholder
import com.vsulimov.resonance.ui.component.ErrorContent
import kotlinx.coroutines.launch

private val ThumbnailSize = 40.dp
private val ThumbnailCornerRadius = 8.dp
private const val SCROLL_TO_TOP_THRESHOLD = 0
private const val PAGINATION_THRESHOLD = 10

/** Separator between artist and album in the supporting text. */
private const val METADATA_SEPARATOR = " \u00B7 "

private const val SECONDS_PER_MINUTE = 60

/**
 * Songs list screen displaying a paginated list of all songs in the library.
 *
 * Each song row shows a cover art thumbnail, song title, artist/album metadata,
 * and formatted duration. Follows M3 list item guidelines for spacing and layout.
 *
 * @param onBack Callback invoked when the back button is tapped.
 * @param onAlbumClick Callback invoked when a song row is tapped, navigating
 *   to the album detail for the song's album.
 * @param modifier Modifier applied to the root layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongListScreen(
    onBack: () -> Unit,
    onAlbumClick: (albumId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: SongListViewModel = viewModel(factory = SongListViewModel.Factory)
    val stateHolder = viewModel.stateHolder
    val screenState by stateHolder.screenState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val showScrollToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > SCROLL_TO_TOP_THRESHOLD }
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleIndex >= totalItems - PAGINATION_THRESHOLD
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) stateHolder.loadMore()
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.song_list_title))
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
                        coroutineScope.launch { listState.animateScrollToItem(0) }
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
            is SongListScreenState.Loading -> LoadingContent(
                modifier = Modifier.padding(innerPadding)
            )
            is SongListScreenState.Content -> SongListContent(
                songs = state.songs,
                isLoadingMore = state.isLoadingMore,
                stateHolder = stateHolder,
                listState = listState,
                onAlbumClick = onAlbumClick,
                modifier = Modifier.padding(innerPadding)
            )
            is SongListScreenState.Error -> ErrorContent(
                title = stringResource(R.string.song_list_error_load_failed),
                onRetry = stateHolder::retry,
                subtitle = state.message.takeIf { it.isNotEmpty() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}

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

@Composable
private fun SongListContent(
    songs: List<SongListItemViewObject>,
    isLoadingMore: Boolean,
    stateHolder: SongListStateHolder,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onAlbumClick: (albumId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize()
    ) {
        items(
            items = songs,
            key = { it.id }
        ) { song ->
            SongListItem(
                song = song,
                loadCoverArt = stateHolder::loadCoverArt,
                getCachedCoverArt = stateHolder::getCachedCoverArt,
                onClick = { song.albumId?.let { onAlbumClick(it) } }
            )
        }

        if (isLoadingMore) {
            item(key = "loading_more") {
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

/**
 * Single song row using M3 [ListItem] for spec-compliant spacing.
 *
 * - Leading: rounded rect cover art thumbnail (40dp)
 * - Headline: song title
 * - Supporting: "Artist · Album"
 * - Trailing: formatted duration
 */
@Composable
private fun SongListItem(
    song: SongListItemViewObject,
    loadCoverArt: suspend (coverArtId: String, sizePx: Int) -> ImageBitmap?,
    getCachedCoverArt: (coverArtId: String) -> ImageBitmap?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var coverArt by remember(song.coverArtId) {
        mutableStateOf(song.coverArtId?.let { getCachedCoverArt(it) })
    }
    val density = LocalDensity.current

    LaunchedEffect(song.coverArtId) {
        if (coverArt == null) {
            song.coverArtId?.let { id ->
                val sizePx = with(density) { ThumbnailSize.roundToPx() }
                coverArt = loadCoverArt(id, sizePx)
            }
        }
    }

    ListItem(
        headlineContent = {
            Text(
                text = song.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = song.artistName + METADATA_SEPARATOR + song.albumName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            SongThumbnail(coverArt = coverArt)
        },
        trailingContent = song.durationSeconds?.let { seconds ->
            {
                Text(
                    text = formatDuration(seconds),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        modifier = modifier
    )
}

/**
 * Rounded rect song thumbnail with cover art placeholder fallback.
 */
@Composable
private fun SongThumbnail(
    coverArt: ImageBitmap?,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(ThumbnailCornerRadius)

    if (coverArt != null) {
        Image(
            bitmap = coverArt,
            contentDescription = null,
            modifier = modifier
                .size(ThumbnailSize)
                .clip(shape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier
                .size(ThumbnailSize)
                .clip(shape)
        ) {
            CoverArtPlaceholder()
        }
    }
}

/**
 * Formats a duration in seconds as "m:ss".
 */
private fun formatDuration(seconds: Int): String {
    val minutes = seconds / SECONDS_PER_MINUTE
    val remainingSeconds = seconds % SECONDS_PER_MINUTE
    return "$minutes:${remainingSeconds.toString().padStart(2, '0')}"
}
