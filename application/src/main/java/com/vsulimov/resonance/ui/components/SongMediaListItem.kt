package com.vsulimov.resonance.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vsulimov.libsubsonic.data.response.browsing.Child
import com.vsulimov.resonance.core.ui.getAppContainer
import com.vsulimov.resonance.core.utils.DownloadUtils
import com.vsulimov.resonance.data.db.DownloadStatus

/**
 * A wrapper around [MediaListItem] that internally observes the download task progress
 * for the given song to prevent high-frequency recompositions of parent lists.
 */
@Composable
fun SongMediaListItem(
    song: Child,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    coverArtId: String? = null,
    placeholderIcon: ImageVector? = null,
    showArtwork: Boolean = true,
    isCircularArtwork: Boolean = false,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onDownloadClick: (() -> Unit)? = null,
    onRemoveDownloadClick: (() -> Unit)? = null
) {
    val appContainer = getAppContainer()
    val task by appContainer.downloadRepository.observeTaskBySongId(song.id).collectAsStateWithLifecycle(null)

    val (status, progress) = DownloadUtils.resolveSongDownloadState(
        songId = song.id,
        suffix = song.suffix,
        downloadTasks = task?.let { listOf(it) } ?: emptyList(),
        storageManager = appContainer.offlineStorageManager
    )

    val isQueuePaused by appContainer.settingsRepository.isDownloadQueuePausedState.collectAsStateWithLifecycle()

    MediaListItem(
        title = title,
        subtitle = subtitle,
        coverArtId = coverArtId,
        placeholderIcon = placeholderIcon,
        onClick = onClick,
        modifier = modifier,
        showArtwork = showArtwork,
        isCircularArtwork = isCircularArtwork,
        isDownloaded = status == DownloadStatus.DOWNLOADED,
        isQueuePaused = isQueuePaused,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        downloadStatus = status,
        downloadProgressProvider = { progress },
        onDownloadClick = onDownloadClick,
        onRemoveDownloadClick = onRemoveDownloadClick
    )
}
