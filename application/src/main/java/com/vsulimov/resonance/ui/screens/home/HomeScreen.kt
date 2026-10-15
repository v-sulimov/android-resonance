package com.vsulimov.resonance.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsulimov.libsubsonic.data.response.browsing.Album
import com.vsulimov.resonance.R
import com.vsulimov.resonance.core.Constants
import com.vsulimov.resonance.core.ui.CoverImage
import com.vsulimov.resonance.core.utils.sanitizeForUi
import com.vsulimov.resonance.ui.components.CircularWavyProgressIndicator
import com.vsulimov.resonance.ui.components.ErrorState
import com.vsulimov.resonance.ui.components.LoadingState
import com.vsulimov.resonance.ui.theme.Dimens

/** Height allocated for two lines of album title and artist text in carousel items. */
private val ALBUM_TEXT_CONTAINER_HEIGHT = 44.sp

/** Size of the inner download icon within active download progress indicator. */
private val DOWNLOAD_PROGRESS_ICON_SIZE = 12.dp

/** Alpha applied to hero subtitle text. */
private const val HERO_SUBTITLE_ALPHA = 0.8f

/** Vertical spacing between album title and artist text in carousel items. */
private val TEXT_LINE_SPACING = 2.dp

/**
 * Main Home screen displaying personalized greeting, hero shuffle card, and music discovery carousels.
 *
 * @param innerPadding Padding values provided by the root Scaffold.
 * @param modifier The modifier to be applied to the layout.
 * @param onProfileClick Callback triggered when user profile icon is clicked.
 * @param onDownloadsClick Callback triggered when active download progress indicator is clicked.
 * @param onAlbumClick Callback triggered when an album item in a carousel is clicked.
 * @param scrollToTopTrigger Incrementing counter that triggers scroll to top when changed.
 * @param viewModel ViewModel holding and managing the Home screen state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    innerPadding: PaddingValues,
    onProfileClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onAlbumClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    scrollToTopTrigger: Int = 0,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val timeOfDay = viewModel.getGreetingTimeOfDay()

    val listState = rememberLazyListState()

    var lastProcessedScrollTrigger by rememberSaveable { mutableIntStateOf(scrollToTopTrigger) }

    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > lastProcessedScrollTrigger) {
            lastProcessedScrollTrigger = scrollToTopTrigger
            listState.animateScrollToItem(0)
        }
    }

    if (uiState.isLoading) {
        LoadingState(
            modifier = modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding()
                )
        )
        return
    }

    val currentError = uiState.error
    val isAllListsEmpty = uiState.recentlyPlayed.isEmpty() &&
        uiState.recentlyAdded.isEmpty() &&
        uiState.mostPlayed.isEmpty() &&
        uiState.randomAlbums.isEmpty() &&
        uiState.starredAlbums.isEmpty()

    if ((currentError != null) && isAllListsEmpty) {
        ErrorState(
            message = currentError.asString(),
            onRetryClick = viewModel::loadData,
            modifier = modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding()
                )
        )
        return
    }

    val safeInsets = WindowInsets.safeDrawing.asPaddingValues()
    val leftInset = safeInsets.calculateLeftPadding(LayoutDirection.Ltr)
    val rightInset = safeInsets.calculateRightPadding(LayoutDirection.Ltr)

    val carouselPadding = PaddingValues(
        start = Dimens.paddingLarge + leftInset,
        end = Dimens.paddingLarge + rightInset
    )

    val topInset = innerPadding.calculateTopPadding()

    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = { viewModel.loadData(isPullToRefresh = true) },
        state = pullToRefreshState,
        modifier = modifier.fillMaxSize(),
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullToRefreshState,
                isRefreshing = uiState.isRefreshing,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = topInset)
            )
        }
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(Dimens.paddingLarge),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + Dimens.paddingSmall,
                bottom = innerPadding.calculateBottomPadding() + Dimens.paddingLarge
            )
        ) {
            item(key = "home_top_bar") {
                HomeTopBar(
                    username = uiState.username,
                    timeOfDay = timeOfDay,
                    hasActiveDownloads = uiState.hasActiveDownloads,
                    isDownloadQueuePaused = uiState.isDownloadQueuePaused,
                    onProfileClick = onProfileClick,
                    onDownloadsClick = onDownloadsClick,
                    modifier = Modifier.padding(
                        start = leftInset,
                        end = rightInset
                    )
                )
            }

            item(key = "hero_action_card") {
                HeroActionCard(
                    onShuffleClick = viewModel::shuffleAll,
                    modifier = Modifier.padding(
                        start = Dimens.paddingLarge + leftInset,
                        end = Dimens.paddingLarge + rightInset
                    )
                )
            }

            if (uiState.recentlyPlayed.isNotEmpty()) {
                item(key = "section_recently_played") {
                    AlbumCarousel(
                        title = stringResource(R.string.home_section_recently_played),
                        albums = uiState.recentlyPlayed,
                        contentPadding = carouselPadding,
                        onAlbumClick = onAlbumClick
                    )
                }
            }

            if (uiState.recentlyAdded.isNotEmpty()) {
                item(key = "section_recently_added") {
                    AlbumCarousel(
                        title = stringResource(R.string.home_section_recently_added),
                        albums = uiState.recentlyAdded,
                        contentPadding = carouselPadding,
                        onAlbumClick = onAlbumClick
                    )
                }
            }

            if (uiState.randomAlbums.isNotEmpty()) {
                item(key = "section_random") {
                    AlbumCarousel(
                        title = stringResource(R.string.home_section_random),
                        albums = uiState.randomAlbums,
                        contentPadding = carouselPadding,
                        onAlbumClick = onAlbumClick
                    )
                }
            }

            if (uiState.starredAlbums.isNotEmpty()) {
                item(key = "section_starred") {
                    AlbumCarousel(
                        title = stringResource(R.string.home_section_starred),
                        isStarred = true,
                        albums = uiState.starredAlbums,
                        contentPadding = carouselPadding,
                        onAlbumClick = onAlbumClick
                    )
                }
            }

            if (uiState.mostPlayed.isNotEmpty()) {
                item(key = "section_most_played") {
                    AlbumCarousel(
                        title = stringResource(R.string.home_section_most_played),
                        albums = uiState.mostPlayed,
                        contentPadding = carouselPadding,
                        onAlbumClick = onAlbumClick
                    )
                }
            }
        }
    }
}

/**
 * Top bar displaying greeting header with current user name, active download progress indicator, and profile action button.
 *
 * @param username Current user's display name.
 * @param timeOfDay Current time of day for personalized greeting message.
 * @param hasActiveDownloads Whether there are active or queued downloads in progress.
 * @param isDownloadQueuePaused Whether the active download queue is currently paused.
 * @param onProfileClick Callback triggered when profile icon is clicked.
 * @param onDownloadsClick Callback triggered when active download progress indicator is clicked.
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
private fun HomeTopBar(
    username: String,
    timeOfDay: TimeOfDay,
    hasActiveDownloads: Boolean,
    isDownloadQueuePaused: Boolean,
    onProfileClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val greetingResId = when (timeOfDay) {
        TimeOfDay.MORNING -> R.string.home_greeting_morning
        TimeOfDay.AFTERNOON -> R.string.home_greeting_afternoon
        TimeOfDay.EVENING -> R.string.home_greeting_evening
        TimeOfDay.NIGHT -> R.string.home_greeting_night
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = Dimens.paddingLarge,
                end = Dimens.paddingLarge - Dimens.paddingSmall,
                top = Dimens.paddingSmall
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(greetingResId, username),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f, fill = false)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasActiveDownloads) {
                IconButton(onClick = onDownloadsClick) {
                    if (isDownloadQueuePaused) {
                        Icon(
                            imageVector = Icons.Outlined.PauseCircle,
                            contentDescription = stringResource(R.string.cd_downloads_paused),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(Dimens.iconMedium)
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            CircularWavyProgressIndicator(
                                progressProvider = null,
                                modifier = Modifier.size(Dimens.iconMedium)
                            )
                            Icon(
                                imageVector = Icons.Outlined.Download,
                                contentDescription = stringResource(R.string.cd_active_downloads_progress),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(DOWNLOAD_PROGRESS_ICON_SIZE)
                            )
                        }
                    }
                }
            }

            IconButton(onClick = onProfileClick) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = stringResource(R.string.cd_profile_icon),
                    modifier = Modifier.size(Dimens.iconLarge),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Hero banner action card offering instant shuffle button.
 *
 * @param onShuffleClick Callback triggered when shuffle all action button is tapped.
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
private fun HeroActionCard(onShuffleClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.paddingLarge),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_hero_subtitle),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = HERO_SUBTITLE_ALPHA)
                )
                Spacer(modifier = Modifier.height(Dimens.paddingSmall / 2))
                Text(
                    text = stringResource(R.string.home_hero_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(Dimens.paddingMedium))

            FilledIconButton(
                onClick = onShuffleClick,
                modifier = Modifier.size(Dimens.iconExtraLarge + Dimens.paddingSmall),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = stringResource(R.string.home_btn_shuffle_all),
                    modifier = Modifier.size(Dimens.iconLarge)
                )
            }
        }
    }
}

/**
 * Horizontal scrolling album list carousel with section title.
 *
 * @param title Section header title string.
 * @param albums List of album domain models.
 * @param contentPadding Horizontal and vertical padding for lazy row items.
 * @param onAlbumClick Callback triggered when an album item is tapped.
 * @param modifier The modifier to be applied to the layout.
 * @param isStarred True if carousel renders favorite/starred albums.
 * @param action Optional trailing header action composable.
 */
@Composable
private fun AlbumCarousel(
    title: String,
    albums: List<Album>,
    contentPadding: PaddingValues,
    onAlbumClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    isStarred: Boolean = false,
    action: (@Composable () -> Unit)? = null
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = contentPadding.calculateLeftPadding(LayoutDirection.Ltr),
                    end = contentPadding.calculateRightPadding(LayoutDirection.Ltr)
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (action != null) {
                action()
            }
        }

        Spacer(modifier = Modifier.height(Dimens.paddingMedium))

        LazyRow(
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(Dimens.paddingMedium)
        ) {
            items(albums, key = { it.id }) { album ->
                AlbumItem(
                    album = album,
                    isStarred = isStarred,
                    onClick = { onAlbumClick(album.id) },
                    modifier = Modifier.width(Dimens.carouselItemWidth)
                )
            }
        }
    }
}

/**
 * Album card item displaying artwork thumbnail, title, and artist name.
 *
 * @param album Album model to render.
 * @param onClick Callback triggered when the album item is clicked.
 * @param modifier The modifier to be applied to the layout.
 * @param isStarred True if a favorite heart badge should be displayed on the artwork.
 */
@Composable
private fun AlbumItem(album: Album, onClick: () -> Unit, modifier: Modifier = Modifier, isStarred: Boolean = false) {
    Column(
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val coverId = album.coverArt
                if (coverId != null) {
                    CoverImage(
                        coverArtId = coverId,
                        size = Constants.COVER_ART_THUMBNAIL_SIZE,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Album,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(Dimens.iconExtraLarge)
                        )
                    }
                }

                if (isStarred) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(Dimens.paddingSmall)
                            .size(Dimens.iconMedium)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(Dimens.iconSmall)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Dimens.paddingSmall))

        val density = LocalDensity.current
        val textContainerHeight = remember(density) {
            with(density) { ALBUM_TEXT_CONTAINER_HEIGHT.toDp() }
        }

        Column(
            modifier = Modifier.height(textContainerHeight),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = album.name.sanitizeForUi(stringResource(R.string.home_unknown_album)),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                minLines = 1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(TEXT_LINE_SPACING))

            Text(
                text = album.artist.sanitizeForUi(stringResource(R.string.home_unknown_artist)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                minLines = 1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
