package com.vsulimov.resonance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.vsulimov.resonance.R
import com.vsulimov.resonance.core.Constants
import com.vsulimov.resonance.core.ui.CoverImage
import com.vsulimov.resonance.data.db.DownloadStatus
import com.vsulimov.resonance.ui.theme.Dimens

private val TEXT_CONTAINER_HEIGHT = 44.sp

/**
 * Reusable grid item composable displaying a square artwork card with title and subtitle.
 *
 * @param title Primary title string.
 * @param subtitle Optional secondary subtitle string.
 * @param coverArtId Optional cover art identifier for remote image loading.
 * @param placeholderIcon Fallback vector icon when cover art is unavailable.
 * @param onClick Callback triggered when the grid item is tapped.
 * @param modifier The modifier to be applied to the layout.
 * @param isDownloaded True if the item is saved for offline playback.
 * @param downloadStatus Optional [DownloadStatus] state to render a download indicator badge.
 * @param isQueuePaused True if active download queue processing is currently paused.
 */
@Composable
fun MediaGridItem(
    title: String,
    subtitle: String?,
    coverArtId: String?,
    placeholderIcon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDownloaded: Boolean = false,
    downloadStatus: DownloadStatus? = null,
    isQueuePaused: Boolean = false
) {
    val showDownloadedBadge = isDownloaded || downloadStatus == DownloadStatus.DOWNLOADED
    val isDownloadingOrQueued = downloadStatus == DownloadStatus.DOWNLOADING || downloadStatus == DownloadStatus.QUEUED

    Column(
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            shape = MaterialTheme.shapes.medium
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
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
                            imageVector = placeholderIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(Dimens.iconExtraLarge)
                        )
                    }
                }

                if (isDownloadingOrQueued) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(Dimens.paddingSmall)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shape = CircleShape
                            )
                            .padding(Dimens.paddingExtraSmall)
                    ) {
                        DownloadStateIndicator(
                            status = downloadStatus,
                            isQueuePaused = isQueuePaused
                        )
                    }
                } else if (showDownloadedBadge) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(Dimens.paddingSmall)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shape = CircleShape
                            )
                            .padding(Dimens.paddingExtraSmall)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = stringResource(R.string.cd_downloaded),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(Dimens.iconExtraSmall)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Dimens.paddingSmall))

        val density = LocalDensity.current
        val textContainerHeight = remember(density) {
            with(density) { TEXT_CONTAINER_HEIGHT.toDp() }
        }

        Column(
            modifier = Modifier.height(textContainerHeight),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                minLines = 1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            subtitle?.let { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    minLines = 1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
