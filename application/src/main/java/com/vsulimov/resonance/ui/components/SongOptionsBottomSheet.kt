package com.vsulimov.resonance.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import com.vsulimov.libsubsonic.data.response.browsing.Child
import com.vsulimov.resonance.R
import com.vsulimov.resonance.core.Constants
import com.vsulimov.resonance.core.ui.CoverImage
import com.vsulimov.resonance.core.utils.sanitizeForUi
import com.vsulimov.resonance.data.db.DownloadStatus
import com.vsulimov.resonance.ui.theme.Dimens

/**
 * Reusable modal bottom sheet displaying action options for a selected song track.
 *
 * Actions include: Play Next, Add to Queue, Download / Remove Download, Toggle Favorite, Go to Album, Go to Artist.
 *
 * @param song The selected [Child] song item.
 * @param onDismiss Callback triggered when the bottom sheet is dismissed.
 * @param onPlayNext Callback triggered when user chooses "Play Next".
 * @param onAddToQueue Callback triggered when user chooses "Add to Queue".
 * @param modifier The modifier to be applied to the layout.
 * @param isFavorite True if the song is currently marked as favorite.
 * @param isDownloaded True if the song is currently downloaded for offline playback.
 * @param downloadStatus Current [DownloadStatus] state of the song item.
 * @param onDownloadClick Optional callback triggered when user taps Download action.
 * @param onRemoveDownloadClick Optional callback triggered when user taps Remove Download action.
 * @param onToggleFavorite Optional callback triggered when user taps favorite action.
 * @param onGoToAlbum Optional callback triggered when user chooses "Go to Album".
 * @param onGoToArtist Optional callback triggered when user chooses "Go to Artist".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongOptionsBottomSheet(
    song: Child,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    isDownloaded: Boolean = false,
    downloadStatus: DownloadStatus = if (isDownloaded) DownloadStatus.DOWNLOADED else DownloadStatus.NOT_DOWNLOADED,
    onDownloadClick: (() -> Unit)? = null,
    onRemoveDownloadClick: (() -> Unit)? = null,
    onToggleFavorite: (() -> Unit)? = null,
    onGoToAlbum: ((String) -> Unit)? = null,
    onGoToArtist: ((String) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val safeDrawingInsets = WindowInsets.safeDrawing.asPaddingValues()
    val leftInset = safeDrawingInsets.calculateLeftPadding(LayoutDirection.Ltr)
    val rightInset = safeDrawingInsets.calculateRightPadding(LayoutDirection.Ltr)

    val isDownloadedOrDownloading = isDownloaded ||
        downloadStatus == DownloadStatus.DOWNLOADED ||
        downloadStatus == DownloadStatus.QUEUED ||
        downloadStatus == DownloadStatus.DOWNLOADING

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = leftInset,
                    end = rightInset,
                    bottom = Dimens.paddingMedium
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = Dimens.paddingMedium,
                        vertical = Dimens.paddingSmall
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val coverArtId = song.coverArt
                if (coverArtId != null) {
                    CoverImage(
                        coverArtId = coverArtId,
                        size = Constants.COVER_ART_THUMBNAIL_SIZE,
                        modifier = Modifier
                            .size(Dimens.artworkSizeListItem)
                            .padding(end = Dimens.paddingMedium)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title.sanitizeForUi(stringResource(R.string.unknown_title)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    song.artist?.let { artist ->
                        Text(
                            text = artist.sanitizeForUi(stringResource(R.string.unknown_artist)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = Dimens.paddingSmall),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.player_option_play_next)) },
                leadingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = null
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable {
                    onPlayNext()
                    onDismiss()
                }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.player_option_add_to_queue)) },
                leadingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                        contentDescription = null
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable {
                    onAddToQueue()
                    onDismiss()
                }
            )

            if (isDownloadedOrDownloading && onRemoveDownloadClick != null) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.download_option_remove)) },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        onRemoveDownloadClick()
                        onDismiss()
                    }
                )
            } else if (!isDownloadedOrDownloading && onDownloadClick != null) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.download_option_download)) },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.Download,
                            contentDescription = null
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        onDownloadClick()
                        onDismiss()
                    }
                )
            }

            if (onToggleFavorite != null) {
                ListItem(
                    headlineContent = {
                        Text(
                            stringResource(
                                if (isFavorite) {
                                    R.string.player_option_remove_favorite
                                } else {
                                    R.string.player_option_add_favorite
                                }
                            )
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isFavorite) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        onToggleFavorite()
                        onDismiss()
                    }
                )
            }

            song.albumId?.let { albumId ->
                if (onGoToAlbum != null) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.player_option_go_to_album)) },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.Album,
                                contentDescription = null
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable {
                            onGoToAlbum(albumId)
                            onDismiss()
                        }
                    )
                }
            }

            song.artistId?.let { artistId ->
                if (onGoToArtist != null) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.player_option_go_to_artist)) },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable {
                            onGoToArtist(artistId)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}
