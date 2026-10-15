package com.vsulimov.resonance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.vsulimov.resonance.R
import com.vsulimov.resonance.core.Constants
import com.vsulimov.resonance.core.ui.CoverImage
import com.vsulimov.resonance.data.db.DownloadStatus
import com.vsulimov.resonance.ui.theme.Dimens

/**
 * Reusable horizontal row list item composable for displaying songs, artists, playlists, or starred items.
 *
 * @param title Primary title string.
 * @param subtitle Optional secondary subtitle text.
 * @param coverArtId Optional cover art identifier for image loading.
 * @param placeholderIcon Fallback vector icon when cover art is unavailable.
 * @param onClick Callback triggered when the list row is clicked.
 * @param modifier The modifier to be applied to the layout.
 * @param showArtwork Whether to display the left artwork card or icon slot.
 * @param isCircularArtwork Whether the artwork avatar should be rendered in a circle shape (e.g. for artists).
 * @param isDownloaded True if the item is downloaded locally for offline playback.
 * @param isQueuePaused True if the active download queue is paused.
 * @param leadingContent Optional composable leading slot (e.g. track index number) rendered instead of artwork.
 * @param trailingContent Optional composable trailing slot for duration, play count, or action menu.
 * @param downloadStatus Optional [DownloadStatus] state to render a download indicator button.
 * @param downloadProgressProvider Optional download progress ratio provider (0f to 1f) when downloading.
 * @param onDownloadClick Optional callback triggered when download button is tapped.
 * @param onRemoveDownloadClick Optional callback triggered when downloaded checkmark button is tapped.
 */
@Composable
fun MediaListItem(
    title: String,
    subtitle: String?,
    coverArtId: String?,
    placeholderIcon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showArtwork: Boolean = true,
    isCircularArtwork: Boolean = false,
    isDownloaded: Boolean = false,
    isQueuePaused: Boolean = false,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    downloadStatus: DownloadStatus? = null,
    downloadProgressProvider: (() -> Float)? = null,
    onDownloadClick: (() -> Unit)? = null,
    onRemoveDownloadClick: (() -> Unit)? = null
) {
    val showDownloadedBadge = isDownloaded || downloadStatus == DownloadStatus.DOWNLOADED
    val isDownloadingOrQueued = downloadStatus == DownloadStatus.DOWNLOADING || downloadStatus == DownloadStatus.QUEUED
    val showActiveIndicator =
        isDownloadingOrQueued || (downloadStatus != null && (onDownloadClick != null || onRemoveDownloadClick != null))

    val endPadding = if (trailingContent != null || showActiveIndicator) {
        Dimens.paddingSmall / 2
    } else {
        Dimens.paddingMedium
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                start = Dimens.paddingMedium,
                end = endPadding,
                top = Dimens.paddingSmall,
                bottom = Dimens.paddingSmall
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingContent != null) {
            leadingContent()
            Spacer(modifier = Modifier.width(Dimens.paddingSmall))
        } else if (showArtwork && (coverArtId != null || placeholderIcon != null)) {
            val shape = if (isCircularArtwork) CircleShape else MaterialTheme.shapes.small

            Card(
                modifier = Modifier.size(Dimens.artworkSizeListItem),
                shape = shape
            ) {
                if (coverArtId != null) {
                    CoverImage(
                        coverArtId = coverArtId,
                        size = Constants.COVER_ART_THUMBNAIL_SIZE,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (placeholderIcon != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = placeholderIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(Dimens.iconLarge)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(Dimens.paddingMedium))
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            subtitle?.let { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (showDownloadedBadge && !showActiveIndicator) {
            Spacer(modifier = Modifier.width(Dimens.paddingSmall))
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = stringResource(R.string.cd_downloaded),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Dimens.iconSmall)
            )
        }

        if (showActiveIndicator && downloadStatus != null) {
            Spacer(modifier = Modifier.width(Dimens.paddingSmall))
            DownloadStateIndicator(
                status = downloadStatus,
                progressProvider = downloadProgressProvider,
                isQueuePaused = isQueuePaused,
                onDownloadClick = onDownloadClick,
                onRemoveClick = onRemoveDownloadClick
            )
        }

        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(Dimens.paddingSmall))
            trailingContent()
        }
    }
}
