package com.vsulimov.resonance.ui.screen.artistlist

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsulimov.resonance.R
import com.vsulimov.resonance.ui.component.CoverArtPlaceholder
import com.vsulimov.resonance.ui.component.ErrorContent
import kotlinx.coroutines.launch

private val ThumbnailSize = 48.dp
private const val SCROLL_TO_TOP_THRESHOLD = 0

/**
 * Artists list screen displaying all artists in a scrollable list.
 *
 * Each artist row shows a circular thumbnail, artist name, and album
 * count. Tapping a row navigates to the artist's album list (TODO).
 *
 * @param onBack Callback invoked when the back button is tapped.
 * @param modifier Modifier applied to the root layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistListScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: ArtistListViewModel = viewModel(factory = ArtistListViewModel.Factory)
    val stateHolder = viewModel.stateHolder
    val screenState by stateHolder.screenState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val showScrollToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > SCROLL_TO_TOP_THRESHOLD }
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.artist_list_title))
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
            is ArtistListScreenState.Loading -> LoadingContent(
                modifier = Modifier.padding(innerPadding)
            )
            is ArtistListScreenState.Content -> ArtistListContent(
                artists = state.artists,
                stateHolder = stateHolder,
                listState = listState,
                modifier = Modifier.padding(innerPadding)
            )
            is ArtistListScreenState.Error -> ErrorContent(
                title = stringResource(R.string.artist_list_error_load_failed),
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
 * Scrollable list of artist rows.
 */
@Composable
private fun ArtistListContent(
    artists: List<ArtistCardViewObject>,
    stateHolder: ArtistListStateHolder,
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize()
    ) {
        items(
            items = artists,
            key = { it.id }
        ) { artist ->
            ArtistListItem(
                artist = artist,
                loadCoverArt = stateHolder::loadCoverArt,
                getCachedCoverArt = stateHolder::getCachedCoverArt
            )
        }
    }
}

/**
 * Single artist row with circular thumbnail, name, and album count.
 */
@Composable
private fun ArtistListItem(
    artist: ArtistCardViewObject,
    loadCoverArt: suspend (coverArtId: String, sizePx: Int) -> ImageBitmap?,
    getCachedCoverArt: (coverArtId: String) -> ImageBitmap?,
    modifier: Modifier = Modifier
) {
    var coverArt by remember(artist.coverArtId) {
        mutableStateOf(artist.coverArtId?.let { getCachedCoverArt(it) })
    }
    val density = LocalDensity.current

    LaunchedEffect(artist.coverArtId) {
        if (coverArt == null) {
            artist.coverArtId?.let { id ->
                val sizePx = with(density) { ThumbnailSize.roundToPx() }
                coverArt = loadCoverArt(id, sizePx)
            }
        }
    }

    ListItem(
        headlineContent = {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        supportingContent = {
            Text(
                text = pluralStringResource(
                    R.plurals.library_album_count,
                    artist.albumCount,
                    artist.albumCount
                ),
                style = MaterialTheme.typography.bodySmall
            )
        },
        leadingContent = {
            ArtistThumbnail(coverArt = coverArt)
        },
        modifier = modifier
    )
}

/**
 * Circular artist thumbnail with placeholder fallback.
 */
@Composable
private fun ArtistThumbnail(
    coverArt: ImageBitmap?,
    modifier: Modifier = Modifier
) {
    if (coverArt != null) {
        Image(
            bitmap = coverArt,
            contentDescription = null,
            modifier = modifier
                .size(ThumbnailSize)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier
                .size(ThumbnailSize)
                .clip(CircleShape)
        ) {
            CoverArtPlaceholder()
        }
    }
}
