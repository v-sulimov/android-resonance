package com.vsulimov.resonance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
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
import com.vsulimov.libsubsonic.data.response.browsing.Album
import com.vsulimov.resonance.R
import com.vsulimov.resonance.core.Constants
import com.vsulimov.resonance.core.ui.CoverImage
import com.vsulimov.resonance.core.utils.sanitizeForUi
import com.vsulimov.resonance.data.db.DownloadStatus
import com.vsulimov.resonance.ui.theme.Dimens

/**
 * Reusable modal bottom sheet displaying action options for a selected album.
 *
 * Actions include: Download / Remove Download, Toggle Favorite, Go to Artist.
 *
 * @param album The target [Album] data object.
 * @param onDismiss Callback triggered when the bottom sheet is dismissed.
 * @param modifier The modifier to be applied to the layout.
 * @param isFavorite True if the album is currently marked as favorite.
 * @param hasDownloadedSongs True if at least one song in the album is downloaded locally.
 * @param downloadStatus Current [DownloadStatus] of the album collection.
 * @param onDownloadClick Optional callback triggered when user taps Download action.
 * @param onRemoveDownloadClick Optional callback triggered when user taps Remove Download action.
 * @param onToggleFavorite Optional callback triggered when user taps Favorite toggle action.
 * @param onGoToArtist Optional callback triggered when user chooses "Go to Artist".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumOptionsBottomSheet(
    album: Album,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    hasDownloadedSongs: Boolean = false,
    downloadStatus: DownloadStatus = DownloadStatus.NOT_DOWNLOADED,
    onDownloadClick: (() -> Unit)? = null,
    onRemoveDownloadClick: (() -> Unit)? = null,
    onToggleFavorite: (() -> Unit)? = null,
    onGoToArtist: ((String) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val safeDrawingInsets = WindowInsets.safeDrawing.asPaddingValues()
    val leftInset = safeDrawingInsets.calculateLeftPadding(LayoutDirection.Ltr)
    val rightInset = safeDrawingInsets.calculateRightPadding(LayoutDirection.Ltr)

    val isFullyDownloaded = downloadStatus == DownloadStatus.DOWNLOADED
    val showDownloadOption = !isFullyDownloaded && onDownloadClick != null
    val showRemoveOption = (hasDownloadedSongs || isFullyDownloaded) && onRemoveDownloadClick != null

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
                Card(
                    modifier = Modifier
                        .size(Dimens.artworkSizeListItem)
                        .padding(end = Dimens.paddingMedium),
                    shape = MaterialTheme.shapes.small
                ) {
                    val coverArtId = album.coverArt
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
                                imageVector = Icons.Default.Album,
                                contentDescription = stringResource(R.string.cd_album_cover),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(Dimens.iconLarge)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = album.name.sanitizeForUi(stringResource(R.string.unknown_album)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = album.artist.sanitizeForUi(stringResource(R.string.unknown_artist)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = Dimens.paddingSmall),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            if (showDownloadOption && onDownloadClick != null) {
                val labelRes = if (hasDownloadedSongs) {
                    R.string.download_option_download_all
                } else {
                    R.string.download_option_download
                }
                ListItem(
                    headlineContent = { Text(stringResource(labelRes)) },
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

            if (showRemoveOption && onRemoveDownloadClick != null) {
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

            album.artistId?.let { artistId ->
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
