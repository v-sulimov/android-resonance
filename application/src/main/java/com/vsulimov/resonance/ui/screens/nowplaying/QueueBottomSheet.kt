package com.vsulimov.resonance.ui.screens.nowplaying

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.vsulimov.libsubsonic.data.response.browsing.Child
import com.vsulimov.resonance.R
import com.vsulimov.resonance.core.utils.DurationFormatter
import com.vsulimov.resonance.core.utils.sanitizeForUi
import com.vsulimov.resonance.ui.components.MediaListItem
import com.vsulimov.resonance.ui.components.PlayingEqualizerIndicator
import com.vsulimov.resonance.ui.theme.Dimens

private val EQUALIZER_INDICATOR_SIZE = 16.dp

/**
 * Modal bottom sheet displaying the current playback queue items and queue control actions.
 *
 * @param queueProvider Current list of [Child] songs in the playback queue.
 * @param currentIndexProvider 0-based index of the currently playing track.
 * @param onDismiss Callback triggered when the sheet is dismissed.
 * @param onItemClick Callback triggered when user taps a song in the queue to jump to it.
 * @param onItemRemove Callback triggered when user removes an item at specified index from the queue.
 * @param onClearQueue Callback triggered when user taps "Clear Queue".
 * @param modifier The modifier to be applied to the layout.
 * @param isPlayingProvider True if playback is currently active to animate the playing indicator.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueBottomSheet(
    queueProvider: () -> List<Child>,
    currentIndexProvider: () -> Int,
    onDismiss: () -> Unit,
    onItemClick: (Int) -> Unit,
    onItemRemove: (Int) -> Unit,
    onClearQueue: () -> Unit,
    modifier: Modifier = Modifier,
    isPlayingProvider: () -> Boolean
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()

    val queue by remember { androidx.compose.runtime.derivedStateOf { queueProvider() } }
    val currentIndex by remember { androidx.compose.runtime.derivedStateOf { currentIndexProvider() } }
    val isPlaying by remember { androidx.compose.runtime.derivedStateOf { isPlayingProvider() } }

    LaunchedEffect(Unit) {
        if (currentIndex in queue.indices) {
            listState.scrollToItem((currentIndex - 1).coerceAtLeast(0))
        }
    }

    val safeDrawingInsets = WindowInsets.safeDrawing.asPaddingValues()
    val leftInset = safeDrawingInsets.calculateLeftPadding(LayoutDirection.Ltr)
    val rightInset = safeDrawingInsets.calculateRightPadding(LayoutDirection.Ltr)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = leftInset,
                    end = rightInset
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.paddingMedium),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.player_queue_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.item_count_format, queue.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (queue.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            onClearQueue()
                            onDismiss()
                        }
                    ) {
                        Text(text = stringResource(R.string.player_queue_clear))
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = Dimens.paddingSmall),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            if (queue.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.artworkSizeMedium),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.player_queue_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val nestedScrollConnection = remember {
                    object : NestedScrollConnection {
                        override fun onPostScroll(
                            consumed: Offset,
                            available: Offset,
                            source: NestedScrollSource
                        ): Offset = Offset(0f, available.y)

                        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
                            Velocity(0f, available.y)
                    }
                }

                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(bottom = Dimens.paddingMedium),
                    modifier = Modifier
                        .fillMaxWidth()
                        .nestedScroll(nestedScrollConnection)
                ) {
                    itemsIndexed(
                        items = queue,
                        key = { index, song -> "${index}_${song.id}" }
                    ) { index, song ->
                        val isCurrent = index == currentIndex

                        MediaListItem(
                            title = song.title.sanitizeForUi(stringResource(R.string.unknown_title)),
                            subtitle = song.artist?.sanitizeForUi(stringResource(R.string.unknown_artist)),
                            coverArtId = song.coverArt,
                            placeholderIcon = null,
                            onClick = { onItemClick(index) },
                            leadingContent = {
                                Box(
                                    modifier = Modifier.width(Dimens.paddingExtraLarge),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isCurrent) {
                                        PlayingEqualizerIndicator(
                                            isPlaying = isPlaying,
                                            size = EQUALIZER_INDICATOR_SIZE
                                        )
                                    } else {
                                        Text(
                                            text = (index + 1).toString(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            trailingContent = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = DurationFormatter.formatSeconds(song.duration),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    IconButton(
                                        onClick = { onItemRemove(index) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
