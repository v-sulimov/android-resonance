package com.vsulimov.resonance.ui.screens.nowplaying

import android.content.res.Configuration
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.vsulimov.libsubsonic.data.response.browsing.Child
import com.vsulimov.resonance.R
import com.vsulimov.resonance.core.Constants
import com.vsulimov.resonance.core.ui.CoverImage
import com.vsulimov.resonance.core.utils.DurationFormatter
import com.vsulimov.resonance.core.utils.sanitizeForUi
import com.vsulimov.resonance.player.model.PlaybackState
import com.vsulimov.resonance.player.model.RepeatMode
import com.vsulimov.resonance.ui.theme.Dimens
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.sin
import kotlinx.coroutines.launch

private val PRIMARY_CONTROLS_ROW_HEIGHT = 88.dp
private val PRIMARY_PLAY_ICON_SIZE = 38.dp
private val PRIMARY_SKIP_ICON_SIZE = 34.dp
private val PRIMARY_ROW_SPACING = 8.dp
private val PRIMARY_ROW_HORIZONTAL_PADDING = 12.dp

private val SECONDARY_ROW_HEIGHT = 52.dp
private val SECONDARY_ICON_SIZE = 24.dp
private val SECONDARY_ROW_SPACING = 8.dp
private val SECONDARY_HORIZONTAL_PADDING = 48.dp
private val SLIDER_THUMB_SIZE = 16.dp

private val EXPRESSIVE_ARTWORK_CORNER_RADIUS = 28.dp
private val EXPRESSIVE_ELEVATION_ARTWORK = 8.dp
private val TITLE_ARTIST_SPACING = 4.dp
private val WAVY_TRACK_HEIGHT = 28.dp
private val WAVY_WAVELENGTH_DP = 22.dp
private val WAVY_AMPLITUDE_DP = 3.5.dp
private val WAVY_STROKE_WIDTH_DP = 4.dp
private const val WAVY_ANIMATION_DURATION_MS = 1200
private val WAVY_STEP_DP = 2.dp

private val PLAY_CORNER_RADIUS_PLAYING = 60.dp
private val PLAY_CORNER_RADIUS_PAUSED = 40.dp

private val TOGGLE_OUTER_CORNER_RADIUS = 28.dp
private val TOGGLE_INNER_CORNER_RADIUS = 12.dp
private val TOGGLE_CENTER_CORNER_RADIUS = 16.dp

/** Outlined vector icon for Skip Previous. */
private val OutlinedSkipPreviousIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "OutlinedSkipPrevious",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).run {
        path(fill = SolidColor(Color.White)) {
            moveTo(6f, 6f)
            horizontalLineToRelative(2f)
            verticalLineToRelative(12f)
            horizontalLineTo(6f)
            close()
            moveTo(9.5f, 12f)
            lineToRelative(8.5f, 6f)
            verticalLineTo(6f)
            close()
            moveTo(16f, 14.14f)
            lineTo(12.97f, 12f)
            lineTo(16f, 9.86f)
            close()
        }
        build()
    }
}

/** Outlined vector icon for Skip Next. */
private val OutlinedSkipNextIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "OutlinedSkipNext",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).run {
        path(fill = SolidColor(Color.White)) {
            moveTo(6f, 18f)
            lineToRelative(8.5f, -6f)
            lineTo(6f, 6f)
            close()
            moveTo(8f, 9.86f)
            lineTo(11.03f, 12f)
            lineTo(8f, 14.14f)
            close()
            moveTo(16f, 6f)
            horizontalLineToRelative(2f)
            verticalLineToRelative(12f)
            horizontalLineToRelative(-2f)
            close()
        }
        build()
    }
}

/**
 * Material Design Expressive full-screen Now Playing modal bottom sheet.
 * Features hero artwork, track metadata (single-line ellipsis), wavy progress slider with round circle thumb,
 * tall round weighted playback controls row with outlined icons and morphing play/pause corners,
 * top-right queue trigger, and a bottom toggle row for shuffle, repeat, and favorite.
 *
 * @param playbackStateProvider Provider for active [PlaybackState] containing song details, progress, and queue.
 * @param onDismiss Callback triggered to collapse the Now Playing screen.
 * @param onPlayPauseClick Callback triggered on play/pause button tap.
 * @param onSkipNextClick Callback triggered on skip next button tap.
 * @param onSkipPreviousClick Callback triggered on skip previous button tap.
 * @param onSeekTo Callback triggered when user releases the progress slider.
 * @param onToggleShuffle Callback triggered on shuffle button tap.
 * @param onToggleRepeat Callback triggered on repeat button tap.
 * @param onRemoveFromQueue Callback triggered when user removes a song from the queue.
 * @param onClearQueue Callback triggered when user clears the queue.
 * @param onPlayQueueItem Callback triggered when user selects a song in the queue.
 * @param modifier The modifier to be applied to the layout.
 * @param starredSongIds Set of starred song IDs for resolving favorite status.
 * @param onToggleFavorite Callback triggered when user taps the favorite button.
 * @param onArtistClick Optional callback triggered on artist name tap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    playbackStateProvider: () -> PlaybackState,
    onDismiss: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onSkipNextClick: () -> Unit,
    onSkipPreviousClick: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    onClearQueue: () -> Unit,
    onPlayQueueItem: (Int) -> Unit,
    modifier: Modifier = Modifier,
    starredSongIds: Set<String> = emptySet(),
    onToggleFavorite: (Child) -> Unit = { _ -> },
    onArtistClick: ((String) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    var showQueueSheet by remember { mutableStateOf(value = false) }

    val songState by remember { androidx.compose.runtime.derivedStateOf { playbackStateProvider().currentSong } }

    val song = songState ?: return

    val collapseSheet: () -> Unit = {
        coroutineScope.launch {
            sheetState.hide()
        }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                onDismiss()
            }
        }
    }

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val safeDrawingInsets = WindowInsets.safeDrawing.asPaddingValues()
    val leftInset = safeDrawingInsets.calculateLeftPadding(LayoutDirection.Ltr)
    val rightInset = safeDrawingInsets.calculateRightPadding(LayoutDirection.Ltr)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = Dimens.paddingLarge + leftInset,
                    end = Dimens.paddingLarge + rightInset,
                    bottom = Dimens.paddingSmall,
                    top = Dimens.paddingSmall
                )
        ) {
            HeaderRow(
                albumName = song.album,
                onCollapseClick = collapseSheet,
                onQueueClick = { showQueueSheet = true }
            )

            val isSongFavorite = starredSongIds.contains(song.id)
            val artistName = song.artist?.sanitizeForUi(stringResource(R.string.unknown_artist))
            val artistId = song.artistId
            val isArtistClickable = (onArtistClick != null) && !artistId.isNullOrBlank()

            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = Dimens.paddingSmall),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.paddingLarge)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(vertical = Dimens.paddingSmall),
                        contentAlignment = Alignment.Center
                    ) {
                        ArtworkHorizontalPager(
                            queueProvider = { playbackStateProvider().queue },
                            queueIndexProvider = { playbackStateProvider().queueIndex },
                            currentSong = song,
                            onSkipToQueueIndex = onPlayQueueItem,
                            modifier = Modifier.fillMaxSize(),
                            artworkModifier = Modifier
                                .fillMaxHeight()
                                .aspectRatio(1f)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TrackTitleAndArtistWithFavorite(
                            title = song.title,
                            artistName = artistName,
                            isArtistClickable = isArtistClickable,
                            isFavorite = isSongFavorite,
                            onArtistClick = {
                                if (artistId != null) {
                                    onArtistClick?.invoke(artistId)
                                    onDismiss()
                                }
                            },
                            onToggleFavorite = { onToggleFavorite(song) }
                        )

                        WavySliderSection(
                            durationMsProvider = { playbackStateProvider().durationMs },
                            currentPosMsProvider = { playbackStateProvider().currentPositionMs },
                            isPlayingProvider = { playbackStateProvider().isPlaying },
                            onSeekTo = onSeekTo
                        )

                        AnimatedPlaybackControlsRow(
                            isPlayingProvider = { playbackStateProvider().isPlaying },
                            isBufferingProvider = { playbackStateProvider().isBuffering },
                            onPlayPauseClick = onPlayPauseClick,
                            onSkipPreviousClick = onSkipPreviousClick,
                            onSkipNextClick = onSkipNextClick
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        ArtworkHorizontalPager(
                            queueProvider = { playbackStateProvider().queue },
                            queueIndexProvider = { playbackStateProvider().queueIndex },
                            currentSong = song,
                            onSkipToQueueIndex = onPlayQueueItem,
                            modifier = Modifier.fillMaxSize(),
                            artworkModifier = Modifier
                                .fillMaxWidth(0.88f)
                                .aspectRatio(1f)
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Dimens.paddingLarge)
                    ) {
                        TrackTitleAndArtist(
                            title = song.title,
                            artistName = artistName,
                            isArtistClickable = isArtistClickable,
                            onArtistClick = {
                                if (artistId != null) {
                                    onArtistClick?.invoke(artistId)
                                    onDismiss()
                                }
                            }
                        )

                        WavySliderSection(
                            durationMsProvider = { playbackStateProvider().durationMs },
                            currentPosMsProvider = { playbackStateProvider().currentPositionMs },
                            isPlayingProvider = { playbackStateProvider().isPlaying },
                            onSeekTo = onSeekTo
                        )

                        AnimatedPlaybackControlsRow(
                            isPlayingProvider = { playbackStateProvider().isPlaying },
                            isBufferingProvider = { playbackStateProvider().isBuffering },
                            onPlayPauseClick = onPlayPauseClick,
                            onSkipPreviousClick = onSkipPreviousClick,
                            onSkipNextClick = onSkipNextClick
                        )

                        BottomToggleRow(
                            shuffleModeEnabledProvider = { playbackStateProvider().shuffleModeEnabled },
                            repeatModeProvider = { playbackStateProvider().repeatMode },
                            isFavorite = isSongFavorite,
                            onToggleShuffle = onToggleShuffle,
                            onToggleRepeat = onToggleRepeat,
                            onToggleFavorite = { onToggleFavorite(song) },
                            modifier = Modifier
                                .padding(horizontal = SECONDARY_HORIZONTAL_PADDING)
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    if (showQueueSheet) {
        QueueBottomSheet(
            queueProvider = { playbackStateProvider().queue },
            currentIndexProvider = { playbackStateProvider().queueIndex },
            isPlayingProvider = { playbackStateProvider().isPlaying },
            onDismiss = { showQueueSheet = false },
            onItemClick = onPlayQueueItem,
            onItemRemove = onRemoveFromQueue,
            onClearQueue = onClearQueue
        )
    }
}

/**
 * Top bar header displaying collapse button, now playing screen title, and queue button on the right.
 */
@Composable
private fun HeaderRow(
    albumName: String?,
    onCollapseClick: () -> Unit,
    onQueueClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.paddingSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onCollapseClick) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = stringResource(R.string.cd_collapse),
                modifier = Modifier.size(Dimens.iconLarge)
            )
        }

        Text(
            text = albumName?.sanitizeForUi(stringResource(R.string.player_now_playing_title))
                ?: stringResource(R.string.player_now_playing_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = Dimens.paddingSmall)
        )

        IconButton(onClick = onQueueClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = stringResource(R.string.cd_show_queue),
                modifier = Modifier.size(Dimens.iconMedium)
            )
        }
    }
}

/**
 * Horizontal scrolling artwork pager supporting gesture swiping across queue track items with scale and alpha page transition animations.
 */
@Composable
private fun ArtworkHorizontalPager(
    queueProvider: () -> List<Child>,
    queueIndexProvider: () -> Int,
    currentSong: Child,
    onSkipToQueueIndex: (Int) -> Unit,
    modifier: Modifier = Modifier,
    artworkModifier: Modifier = Modifier
) {
    val displayQueue = remember(queueProvider(), currentSong) {
        queueProvider().ifEmpty { listOf(currentSong) }
    }

    val displayIndex = remember(queueIndexProvider(), displayQueue, currentSong) {
        if (queueIndexProvider() in displayQueue.indices) {
            queueIndexProvider()
        } else {
            displayQueue.indexOfFirst { it.id == currentSong.id }.coerceAtLeast(0)
        }
    }

    val pagerState = rememberPagerState(
        initialPage = displayIndex,
        pageCount = { displayQueue.size }
    )

    LaunchedEffect(displayIndex, displayQueue) {
        if (displayIndex in displayQueue.indices && pagerState.currentPage != displayIndex) {
            pagerState.animateScrollToPage(displayIndex)
        }
    }

    LaunchedEffect(pagerState.settledPage) {
        val targetIndex = pagerState.settledPage
        if (targetIndex != displayIndex && targetIndex in displayQueue.indices) {
            onSkipToQueueIndex(targetIndex)
        }
    }

    HorizontalPager(
        state = pagerState,
        key = { page -> "${page}_${displayQueue.getOrNull(page)?.id}" },
        modifier = modifier
    ) { page ->
        val pageSong = displayQueue.getOrNull(page) ?: currentSong

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            ExpressiveArtworkCard(
                coverArtId = pageSong.coverArt,
                modifier = artworkModifier.graphicsLayer {
                    val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                    val scale = (1f - (0.15f * pageOffset.absoluteValue)).coerceIn(0.85f, 1f)
                    val alpha = (1f - (0.5f * pageOffset.absoluteValue)).coerceIn(0.5f, 1f)
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
            )
        }
    }
}

/**
 * Hero artwork card styled according to Material 3 Expressive guidelines with expressive rounded corners.
 */
@Composable
private fun ExpressiveArtworkCard(coverArtId: String?, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(EXPRESSIVE_ARTWORK_CORNER_RADIUS),
        elevation = CardDefaults.cardElevation(defaultElevation = EXPRESSIVE_ELEVATION_ARTWORK)
    ) {
        if (coverArtId != null) {
            CoverImage(
                coverArtId = coverArtId,
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
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Dimens.iconExtraLarge)
                )
            }
        }
    }
}

/**
 * Displays track title and artist name with single-line maxLines and ellipsis truncation.
 */
@Composable
private fun TrackTitleAndArtist(
    title: String,
    artistName: String?,
    isArtistClickable: Boolean,
    onArtistClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.paddingMedium),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title.sanitizeForUi(stringResource(R.string.unknown_title)),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(TITLE_ARTIST_SPACING))

        artistName?.let { name ->
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isArtistClickable) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable(enabled = isArtistClickable) {
                    onArtistClick()
                }
            )
        }
    }
}

/**
 * Displays track title and artist name alongside an inline favorite heart button for landscape view.
 */
@Composable
private fun TrackTitleAndArtistWithFavorite(
    title: String,
    artistName: String?,
    isArtistClickable: Boolean,
    isFavorite: Boolean,
    onArtistClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title.sanitizeForUi(stringResource(R.string.unknown_title)),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            artistName?.let { name ->
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isArtistClickable) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable(enabled = isArtistClickable) {
                        onArtistClick()
                    }
                )
            }
        }

        IconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = stringResource(
                    if (isFavorite) R.string.cd_remove_from_favorites else R.string.cd_add_to_favorites
                ),
                tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Wavy progress slider section displaying an animated squiggly waveform progress line with a round thumb and durations.
 */
@Composable
private fun WavySliderSection(
    durationMsProvider: () -> Long,
    currentPosMsProvider: () -> Long,
    isPlayingProvider: () -> Boolean,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDraggingSlider by remember { mutableStateOf(value = false) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }

    val displayedPosition = if (isDraggingSlider) {
        (sliderPosition * durationMsProvider()).toLong()
    } else {
        currentPosMsProvider()
    }

    val currentFraction = if (durationMsProvider() > 0) {
        if (isDraggingSlider) {
            sliderPosition
        } else {
            (currentPosMsProvider().toFloat() / durationMsProvider().toFloat()).coerceIn(0f, 1f)
        }
    } else {
        0f
    }

    Column(modifier = modifier.fillMaxWidth()) {
        WavySlider(
            value = currentFraction,
            isPlayingProvider = isPlayingProvider,
            onValueChange = { fraction ->
                isDraggingSlider = true
                sliderPosition = fraction
            },
            onValueChangeFinished = {
                onSeekTo((sliderPosition * durationMsProvider()).toLong())
                isDraggingSlider = false
            },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = DurationFormatter.formatSeconds((displayedPosition / 1000L).toInt()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = DurationFormatter.formatSeconds((durationMsProvider() / 1000L).toInt()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Custom Material 3 Wavy Slider rendering an animated sine wave for active track with a round circle thumb.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WavySlider(
    value: Float,
    isPlayingProvider: () -> Boolean,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "WavyProgressTransition")
    val wavePhase by if (isPlayingProvider()) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = WAVY_ANIMATION_DURATION_MS, easing = LinearEasing)
            ),
            label = "WavePhase"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest

    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        enabled = enabled,
        modifier = modifier,
        thumb = {
            Box(
                modifier = Modifier
                    .size(SLIDER_THUMB_SIZE)
                    .background(
                        color = primaryColor,
                        shape = CircleShape
                    )
            )
        },
        track = { sliderState ->
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(WAVY_TRACK_HEIGHT)
            ) {
                val width = size.width
                val height = size.height
                val centerY = height / 2f

                val activeFraction = sliderState.value.coerceIn(0f, 1f)
                val activeWidth = width * activeFraction

                val strokeWidth = WAVY_STROKE_WIDTH_DP.toPx()
                val wavelength = WAVY_WAVELENGTH_DP.toPx()
                val maxAmplitude = WAVY_AMPLITUDE_DP.toPx()

                if (activeWidth < width) {
                    drawLine(
                        color = inactiveTrackColor,
                        start = Offset(activeWidth, centerY),
                        end = Offset(width, centerY),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }

                if (activeWidth > 0f) {
                    val path = Path()
                    var x = 0f
                    val step = WAVY_STEP_DP.toPx()
                    val fadeMargin = WAVY_WAVELENGTH_DP.toPx().coerceAtMost(activeWidth / 2f)

                    path.moveTo(0f, centerY)

                    while (x <= activeWidth) {
                        val distFromEdge = minOf(x, activeWidth - x)
                        val edgeFactor = if (fadeMargin > 0f) (distFromEdge / fadeMargin).coerceIn(0f, 1f) else 1f
                        val dampening = edgeFactor * edgeFactor * (3f - (2f * edgeFactor))
                        val amplitude = maxAmplitude * dampening

                        val y = centerY + (amplitude * sin(((x / wavelength) * 2 * PI) + wavePhase)).toFloat()
                        path.lineTo(x, y)
                        x += step
                    }

                    drawPath(
                        path = path,
                        color = primaryColor,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }
        }
    )
}

/**
 * Material Design Expressive weighted playback controls row with outlined icons (Skip Previous, Play/Pause, Skip Next).
 * Each button occupies equal weight across the available width with an expressive morphing corner radius for Play/Pause.
 */
@Composable
private fun AnimatedPlaybackControlsRow(
    isPlayingProvider: () -> Boolean,
    isBufferingProvider: () -> Boolean,
    onPlayPauseClick: () -> Unit,
    onSkipPreviousClick: () -> Unit,
    onSkipNextClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEffectivelyPlaying = isPlayingProvider() || isBufferingProvider()
    val playCornerRadius by animateDpAsState(
        targetValue = if (isEffectivelyPlaying) PLAY_CORNER_RADIUS_PLAYING else PLAY_CORNER_RADIUS_PAUSED,
        label = "playCornerRadius"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PRIMARY_ROW_HORIZONTAL_PADDING)
            .height(PRIMARY_CONTROLS_ROW_HEIGHT),
        horizontalArrangement = Arrangement.spacedBy(PRIMARY_ROW_SPACING),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .clickable { onSkipPreviousClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = OutlinedSkipPreviousIcon,
                contentDescription = stringResource(R.string.cd_skip_previous),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(PRIMARY_SKIP_ICON_SIZE)
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(playCornerRadius))
                .background(MaterialTheme.colorScheme.primary)
                .clickable { onPlayPauseClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isEffectivelyPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                contentDescription = if (isEffectivelyPlaying) {
                    stringResource(R.string.cd_pause)
                } else {
                    stringResource(R.string.cd_play)
                },
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(PRIMARY_PLAY_ICON_SIZE)
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .clickable { onSkipNextClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = OutlinedSkipNextIcon,
                contentDescription = stringResource(R.string.cd_skip_next),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(PRIMARY_SKIP_ICON_SIZE)
            )
        }
    }
}

/**
 * Material Design Expressive compact bottom toggle row enclosing shuffle, favorite, and repeat inside a surface container.
 * Features segmented shapes: rounded outer corners for left/right toggles and a rounded rectangle for the center favorite toggle.
 */
@Composable
private fun BottomToggleRow(
    shuffleModeEnabledProvider: () -> Boolean,
    repeatModeProvider: () -> RepeatMode,
    isFavorite: Boolean,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shuffleModeEnabled = shuffleModeEnabledProvider()
    val repeatMode = repeatModeProvider()
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.paddingSmall),
            horizontalArrangement = Arrangement.spacedBy(SECONDARY_ROW_SPACING),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val shuffleContainerColor = if (shuffleModeEnabled) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            }
            val shuffleContentColor = if (shuffleModeEnabled) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(SECONDARY_ROW_HEIGHT)
                    .clip(
                        RoundedCornerShape(
                            topStart = TOGGLE_OUTER_CORNER_RADIUS,
                            bottomStart = TOGGLE_OUTER_CORNER_RADIUS,
                            topEnd = TOGGLE_INNER_CORNER_RADIUS,
                            bottomEnd = TOGGLE_INNER_CORNER_RADIUS
                        )
                    )
                    .background(shuffleContainerColor)
                    .clickable { onToggleShuffle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shuffle,
                    contentDescription = stringResource(R.string.cd_shuffle),
                    tint = shuffleContentColor,
                    modifier = Modifier.size(SECONDARY_ICON_SIZE)
                )
            }

            val favoriteContainerColor = if (isFavorite) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            }
            val favoriteContentColor = if (isFavorite) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(SECONDARY_ROW_HEIGHT)
                    .clip(RoundedCornerShape(TOGGLE_CENTER_CORNER_RADIUS))
                    .background(favoriteContainerColor)
                    .clickable { onToggleFavorite() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = stringResource(
                        if (isFavorite) R.string.cd_remove_from_favorites else R.string.cd_add_to_favorites
                    ),
                    tint = favoriteContentColor,
                    modifier = Modifier.size(SECONDARY_ICON_SIZE)
                )
            }

            val isRepeatActive = repeatMode != RepeatMode.OFF
            val (repeatIcon, repeatCdRes) = when (repeatMode) {
                RepeatMode.OFF -> Pair(Icons.Outlined.Repeat, R.string.player_repeat_off)
                RepeatMode.ALL -> Pair(Icons.Outlined.Repeat, R.string.player_repeat_all)
                RepeatMode.ONE -> Pair(Icons.Outlined.RepeatOne, R.string.player_repeat_one)
            }

            val repeatContainerColor = if (isRepeatActive) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            }
            val repeatContentColor = if (isRepeatActive) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(SECONDARY_ROW_HEIGHT)
                    .clip(
                        RoundedCornerShape(
                            topStart = TOGGLE_INNER_CORNER_RADIUS,
                            bottomStart = TOGGLE_INNER_CORNER_RADIUS,
                            topEnd = TOGGLE_OUTER_CORNER_RADIUS,
                            bottomEnd = TOGGLE_OUTER_CORNER_RADIUS
                        )
                    )
                    .background(repeatContainerColor)
                    .clickable { onToggleRepeat() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = repeatIcon,
                    contentDescription = stringResource(repeatCdRes),
                    tint = repeatContentColor,
                    modifier = Modifier.size(SECONDARY_ICON_SIZE)
                )
            }
        }
    }
}
