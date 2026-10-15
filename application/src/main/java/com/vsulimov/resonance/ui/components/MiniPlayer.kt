package com.vsulimov.resonance.ui.components

import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.vsulimov.libsubsonic.data.response.browsing.Child
import com.vsulimov.resonance.R
import com.vsulimov.resonance.core.Constants
import com.vsulimov.resonance.core.ui.CoverImage
import com.vsulimov.resonance.core.utils.sanitizeForUi
import com.vsulimov.resonance.ui.theme.Dimens
import kotlin.math.PI
import kotlin.math.sin

private val MINI_PLAYER_HEIGHT = 64.dp
private val PADDING_EXTRA_SMALL = 4.dp
private val MINI_PLAYER_ELEVATION = 4.dp
private val EXPRESSIVE_CORNER_RADIUS = 20.dp
private val ARTWORK_CORNER_RADIUS = 12.dp

private val CONTROL_BUTTON_SIZE = 40.dp
private val PLAY_ICON_SIZE = 24.dp
private val FAVORITE_ICON_SIZE = 20.dp

private val WAVY_PROGRESS_HEIGHT = 8.dp
private val WAVY_WAVELENGTH_DP = 22.dp
private val WAVY_AMPLITUDE_DP = 2.5.dp
private val WAVY_STROKE_WIDTH_DP = 3.dp
private const val MINI_PLAYER_WAVY_ANIMATION_DURATION_MS = 1200
private val WAVY_STEP_DP = 2.dp

/**
 * Collapsible floating Mini-Player bar component displaying current track, progress, and playback controls.
 *
 * Styled according to Material 3 Expressive guidelines with expressive rounded corners,
 * vibrant circular play/pause button, pale circular favorite toggle, outlined icons, and
 * an animated wavy progress bar underneath matching the full-screen player style.
 *
 * Supports horizontal swiping across playback queue items with page transition animations.
 * Designed to dock above the navigation bar in [com.vsulimov.resonance.ui.navigation.MainShell].
 *
 * @param song Currently active [Child] song metadata.
 * @param isPlaying True if audio is actively playing.
 * @param isBuffering True if media player is buffering.
 * @param progressFractionProvider Playback progress fraction provider in range [0.0, 1.0].
 * @param onPlayPauseClick Callback triggered when play/pause button is tapped.
 * @param onClick Callback triggered when user taps the Mini-Player card to expand Now Playing screen.
 * @param modifier The modifier to be applied to the layout.
 * @param queue Optional list of [Child] items in the active playback queue.
 * @param queueIndex 0-based index of the currently playing track in [queue].
 * @param starredSongIds Set of starred song IDs for resolving favorite icon state.
 * @param onToggleFavorite Callback triggered when user taps the favorite (heart) icon for a track.
 * @param onSkipToQueueIndex Callback triggered when user swipes to a target queue item index.
 */
@Composable
fun MiniPlayer(
    song: Child,
    isPlaying: Boolean,
    isBuffering: Boolean,
    progressFractionProvider: () -> Float,
    onPlayPauseClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    queue: List<Child> = emptyList(),
    queueIndex: Int = -1,
    starredSongIds: Set<String> = emptySet(),
    onToggleFavorite: (Child) -> Unit = {},
    onSkipToQueueIndex: (Int) -> Unit = {}
) {
    val displayQueue = remember(queue, song) {
        queue.ifEmpty { listOf(song) }
    }

    val displayIndex = remember(queueIndex, displayQueue, song) {
        if (queueIndex in displayQueue.indices) {
            queueIndex
        } else {
            displayQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        }
    }

    val pagerState = rememberPagerState(initialPage = displayIndex) {
        displayQueue.size
    }

    LaunchedEffect(displayIndex, displayQueue) {
        if ((displayIndex in displayQueue.indices) && (pagerState.currentPage != displayIndex)) {
            pagerState.scrollToPage(displayIndex)
        }
    }

    LaunchedEffect(pagerState.settledPage) {
        val targetIndex = pagerState.settledPage
        if ((targetIndex != displayIndex) && (targetIndex in displayQueue.indices)) {
            onSkipToQueueIndex(targetIndex)
        }
    }

    val safeDrawingInsets = WindowInsets.safeDrawing.asPaddingValues()
    val leftInset = safeDrawingInsets.calculateLeftPadding(LayoutDirection.Ltr)
    val rightInset = safeDrawingInsets.calculateRightPadding(LayoutDirection.Ltr)

    val containerShape = RoundedCornerShape(EXPRESSIVE_CORNER_RADIUS)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = Dimens.paddingMedium + leftInset,
                end = Dimens.paddingMedium + rightInset,
                top = PADDING_EXTRA_SMALL,
                bottom = PADDING_EXTRA_SMALL
            )
            .clip(containerShape),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = MINI_PLAYER_ELEVATION,
        shape = containerShape
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            HorizontalPager(
                state = pagerState,
                key = { page -> "${page}_${displayQueue.getOrNull(page)?.id}" },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MINI_PLAYER_HEIGHT)
            ) { page ->
                val pageSong = displayQueue.getOrNull(page) ?: song
                val isCurrentPage = page == displayIndex

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(onClick = onClick)
                        .padding(horizontal = Dimens.paddingMedium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val coverArtId = pageSong.coverArt

                    Card(
                        modifier = Modifier.size(Dimens.artworkSizeListItem),
                        shape = RoundedCornerShape(ARTWORK_CORNER_RADIUS)
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
                                    .padding(PADDING_EXTRA_SMALL),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(Dimens.paddingMedium))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = pageSong.title.sanitizeForUi(stringResource(R.string.unknown_title)),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        pageSong.artist?.let { artist ->
                            Text(
                                text = artist.sanitizeForUi(stringResource(R.string.unknown_artist)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(Dimens.paddingSmall))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.paddingSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isEffectivelyPlaying = isCurrentPage && (isPlaying || isBuffering)

                        Box(
                            modifier = Modifier
                                .size(CONTROL_BUTTON_SIZE)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable {
                                    if (isCurrentPage) {
                                        onPlayPauseClick()
                                    } else {
                                        onSkipToQueueIndex(page)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isEffectivelyPlaying) {
                                    Icons.Outlined.Pause
                                } else {
                                    Icons.Outlined.PlayArrow
                                },
                                contentDescription = if (isEffectivelyPlaying) {
                                    stringResource(R.string.cd_pause)
                                } else {
                                    stringResource(R.string.cd_play)
                                },
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(PLAY_ICON_SIZE)
                            )
                        }

                        val isPageSongFavorite = starredSongIds.contains(pageSong.id)

                        Box(
                            modifier = Modifier
                                .size(CONTROL_BUTTON_SIZE)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .clickable { onToggleFavorite(pageSong) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPageSongFavorite) {
                                    Icons.Filled.Favorite
                                } else {
                                    Icons.Outlined.FavoriteBorder
                                },
                                contentDescription = stringResource(
                                    if (isPageSongFavorite) {
                                        R.string.cd_remove_from_favorites
                                    } else {
                                        R.string.cd_add_to_favorites
                                    }
                                ),
                                tint = if (isPageSongFavorite) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                },
                                modifier = Modifier.size(FAVORITE_ICON_SIZE)
                            )
                        }
                    }
                }
            }

            WavyLinearProgressIndicator(
                progressFractionProvider = progressFractionProvider,
                isPlaying = isPlaying || isBuffering,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Animated wavy linear progress indicator for Mini-Player bottom edge matching full-screen slider wave style.
 * Draws an animated sine waveform when audio is actively playing, transitioning
 * smoothly to a flat line when paused.
 *
 * @param progressFractionProvider Playback progress in range [0.0, 1.0].
 * @param isPlaying True if audio is playing or buffering.
 * @param modifier The modifier to be applied to the layout.
 * @param primaryColor Color for active progress wave segment.
 * @param trackColor Color for inactive track segment.
 */
@Composable
private fun WavyLinearProgressIndicator(
    progressFractionProvider: () -> Float,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val infiniteTransition = rememberInfiniteTransition(label = "MiniPlayerWavyProgressTransition")
    val wavePhase by if (isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = MINI_PLAYER_WAVY_ANIMATION_DURATION_MS, easing = LinearEasing)
            ),
            label = "WavePhase"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(WAVY_PROGRESS_HEIGHT)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f

        val activeFraction = progressFractionProvider().coerceIn(0f, 1f)
        val activeWidth = width * activeFraction

        val strokeWidth = WAVY_STROKE_WIDTH_DP.toPx()
        val wavelength = WAVY_WAVELENGTH_DP.toPx()
        val maxAmplitude = WAVY_AMPLITUDE_DP.toPx()

        if (activeWidth < width) {
            drawLine(
                color = trackColor,
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
            val fadeMargin = wavelength.coerceAtMost(activeWidth / 2f)

            path.moveTo(0f, centerY)

            while (x <= activeWidth) {
                val distFromEdge = minOf(x, activeWidth - x)
                val edgeFactor = if (fadeMargin > 0f) (distFromEdge / fadeMargin).coerceIn(0f, 1f) else 1f
                val dampening = edgeFactor * edgeFactor * (3f - (2f * edgeFactor))
                val amplitude = if (isPlaying) maxAmplitude * dampening else 0f

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
