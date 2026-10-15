package com.vsulimov.resonance.ui.screens.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsulimov.resonance.R
import com.vsulimov.resonance.core.utils.FileUtils
import com.vsulimov.resonance.core.utils.sanitizeForUi
import com.vsulimov.resonance.data.db.DownloadStatus
import com.vsulimov.resonance.ui.components.DownloadStateIndicator
import com.vsulimov.resonance.ui.theme.Dimens

/**
 * Stateful entry composable for the Downloads & Storage Management Overview Screen.
 *
 * @param innerPadding Padding values provided by outer scaffold.
 * @param onBackClick Callback triggered when back navigation is tapped.
 * @param onManageDownloadsClick Callback triggered when user taps Manage Downloads navigation card.
 * @param modifier The modifier to be applied to the layout.
 * @param viewModel ViewModel holding screen state.
 */
@Composable
fun DownloadsScreen(
    innerPadding: PaddingValues,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onManageDownloadsClick: () -> Unit = {},
    viewModel: DownloadsViewModel = viewModel(factory = DownloadsViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DownloadsContent(
        uiState = uiState,
        innerPadding = innerPadding,
        onBackClick = onBackClick,
        onManageDownloadsClick = onManageDownloadsClick,
        onDownloadLibraryClick = { viewModel.setDownloadLibraryDialogOpen(true) },
        onRemoveAllClick = { viewModel.setRemoveAllDialogOpen(true) },
        onPauseDownloads = viewModel::pauseDownloads,
        onResumeDownloads = viewModel::resumeDownloads,
        onCancelDownload = viewModel::cancelDownload,
        onCancelAllDownloads = viewModel::cancelAllDownloads,
        onClearFailed = viewModel::clearFailedDownloads,
        viewModel = viewModel,
        modifier = modifier
    )

    if (uiState.isDownloadLibraryDialogOpen) {
        AlertDialog(
            onDismissRequest = { viewModel.setDownloadLibraryDialogOpen(false) },
            title = { Text(stringResource(R.string.dialog_download_library_title)) },
            text = { Text(stringResource(R.string.dialog_download_library_message)) },
            confirmButton = {
                Button(onClick = { viewModel.downloadEntireLibrary() }) {
                    Text(stringResource(R.string.dialog_download_library_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setDownloadLibraryDialogOpen(false) }) {
                    Text(stringResource(R.string.dialog_download_library_cancel))
                }
            }
        )
    }

    if (uiState.isRemoveAllDialogOpen) {
        val totalSongCount = uiState.downloadedSongs.size
        val formattedSize = FileUtils.formatFileSize(uiState.totalOfflineStorageBytes)

        AlertDialog(
            onDismissRequest = { viewModel.setRemoveAllDialogOpen(false) },
            title = { Text(stringResource(R.string.dialog_remove_all_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.dialog_remove_all_message,
                        formattedSize,
                        totalSongCount
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = viewModel::removeAllDownloads,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(R.string.dialog_remove_all_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setRemoveAllDialogOpen(false) }) {
                    Text(stringResource(R.string.dialog_remove_download_cancel))
                }
            }
        )
    }
}

/**
 * Stateless content composable displaying storage statistics, active download queue, and manage downloads entry card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsContent(
    uiState: DownloadsUiState,
    innerPadding: PaddingValues,
    onBackClick: () -> Unit,
    onManageDownloadsClick: () -> Unit,
    onDownloadLibraryClick: () -> Unit,
    onRemoveAllClick: () -> Unit,
    onPauseDownloads: () -> Unit,
    onResumeDownloads: () -> Unit,
    onCancelDownload: (String) -> Unit,
    onCancelAllDownloads: () -> Unit,
    onClearFailed: () -> Unit,
    viewModel: DownloadsViewModel,
    modifier: Modifier = Modifier
) {
    val safeInsets = WindowInsets.safeDrawing.asPaddingValues()
    var isMenuOpen by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.downloads_screen_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { isMenuOpen = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = null
                        )
                    }
                    DropdownMenu(
                        expanded = isMenuOpen,
                        onDismissRequest = { isMenuOpen = false }
                    ) {
                        if (!uiState.isLibraryFullyDownloaded) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.downloads_menu_download_library)) },
                                onClick = {
                                    isMenuOpen = false
                                    onDownloadLibraryClick()
                                }
                            )
                        }
                        if (uiState.downloadedSongs.isNotEmpty()) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.downloads_menu_remove_all),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    isMenuOpen = false
                                    onRemoveAllClick()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { scaffoldPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = safeInsets.calculateLeftPadding(LayoutDirection.Ltr) + Dimens.paddingMedium,
                top = scaffoldPadding.calculateTopPadding() + Dimens.paddingSmall,
                end = safeInsets.calculateRightPadding(LayoutDirection.Ltr) + Dimens.paddingMedium,
                bottom = innerPadding.calculateBottomPadding() + Dimens.paddingLarge
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.paddingMedium)
        ) {
            val isHeroVisible = uiState.downloadedSongs.isEmpty() &&
                !uiState.isLibraryFullyDownloaded &&
                uiState.activeTaskIds.isEmpty()

            if (isHeroVisible) {
                item(key = "download_hero_card") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Dimens.paddingLarge)
                        ) {
                            Text(
                                text = stringResource(R.string.downloads_hero_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(Dimens.paddingSmall))
                            Text(
                                text = stringResource(R.string.downloads_hero_desc),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(Dimens.paddingMedium))
                            Button(
                                onClick = onDownloadLibraryClick,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.downloads_btn_start_library))
                            }
                        }
                    }
                }
            }

            item(key = "storage_card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimens.paddingMedium)
                    ) {
                        Text(
                            text = stringResource(R.string.downloads_section_storage),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(Dimens.paddingSmall))

                        StorageRow(
                            label = stringResource(R.string.downloads_storage_audio),
                            value = FileUtils.formatFileSize(uiState.audioStorageBytes)
                        )
                        StorageRow(
                            label = stringResource(R.string.downloads_storage_covers),
                            value = FileUtils.formatFileSize(uiState.coversStorageBytes)
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.paddingSmall))
                        StorageRow(
                            label = stringResource(R.string.downloads_storage_total),
                            value = FileUtils.formatFileSize(uiState.totalOfflineStorageBytes),
                            isBold = true
                        )
                        StorageRow(
                            label = stringResource(R.string.downloads_storage_free),
                            value = FileUtils.formatFileSize(uiState.freeDiskBytes)
                        )
                    }
                }
            }

            item(key = "manage_downloads_card") {
                Card(
                    onClick = onManageDownloadsClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimens.paddingMedium),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(Dimens.iconExtraLarge)
                                .background(
                                    color = MaterialTheme.colorScheme.tertiary,
                                    shape = MaterialTheme.shapes.medium
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiary,
                                modifier = Modifier.size(Dimens.iconMedium)
                            )
                        }

                        Spacer(modifier = Modifier.width(Dimens.paddingMedium))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.manage_downloads_nav_card_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(Dimens.paddingSmall / 2))
                            Text(
                                text = stringResource(R.string.manage_downloads_nav_card_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                            )
                        }

                        Spacer(modifier = Modifier.width(Dimens.paddingSmall))

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            val activeTasksCount = uiState.activeTaskIds.size

            if (activeTasksCount > 0) {
                item(key = "queue_header") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.downloads_section_queue, activeTasksCount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(Dimens.paddingSmall))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Dimens.paddingExtraSmall),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (uiState.isQueuePaused) {
                                OutlinedButton(
                                    onClick = onResumeDownloads,
                                    contentPadding = PaddingValues(
                                        horizontal = Dimens.paddingSmall,
                                        vertical = Dimens.paddingExtraSmall
                                    )
                                ) {
                                    Text(
                                        text = stringResource(R.string.downloads_action_resume),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            } else {
                                OutlinedButton(
                                    onClick = onPauseDownloads,
                                    contentPadding = PaddingValues(
                                        horizontal = Dimens.paddingSmall,
                                        vertical = Dimens.paddingExtraSmall
                                    )
                                ) {
                                    Text(
                                        text = stringResource(R.string.downloads_action_pause),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = onCancelAllDownloads,
                                contentPadding = PaddingValues(
                                    horizontal = Dimens.paddingSmall,
                                    vertical = Dimens.paddingExtraSmall
                                )
                            ) {
                                Text(
                                    text = stringResource(R.string.download_notification_action_cancel),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            // Clear Failed button logic moved inside row to avoid tracking failure state at parent level,
                            // or we can just always show it if activeTasksCount > 0 and let it be a no-op if no failed.
                            // To be precise without breaking recomposition, we can just show it.
                            TextButton(
                                onClick = onClearFailed,
                                contentPadding = PaddingValues(
                                    horizontal = Dimens.paddingSmall,
                                    vertical = Dimens.paddingExtraSmall
                                )
                            ) {
                                Text(
                                    text = stringResource(R.string.downloads_action_clear_failed),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                items(
                    items = uiState.activeTaskIds,
                    key = { songId -> songId }
                ) { songId ->
                    val taskWithSong by viewModel.observeTaskWithSong(
                        songId
                    ).collectAsStateWithLifecycle(initialValue = null)

                    taskWithSong?.let { item ->
                        DownloadTaskRow(
                            taskWithSong = item,
                            isQueuePaused = uiState.isQueuePaused,
                            onCancelClick = { onCancelDownload(songId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageRow(label: String, value: String, modifier: Modifier = Modifier, isBold: Boolean = false) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.paddingSmall / 2),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DownloadTaskRow(
    taskWithSong: DownloadTaskWithSong,
    isQueuePaused: Boolean,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val task = taskWithSong.task
    val song = taskWithSong.song

    val titleText = song?.title?.sanitizeForUi(stringResource(R.string.unknown_title)) ?: task.songId
    val artistName = song?.artist?.sanitizeForUi(stringResource(R.string.unknown_artist))

    val statusSubtitle = if (task.status == DownloadStatus.DOWNLOADING && task.totalBytes > 0) {
        val downloadedText = FileUtils.formatFileSize(task.bytesDownloaded)
        val totalText = FileUtils.formatFileSize(task.totalBytes)
        if (artistName != null) {
            "$artistName • $downloadedText / $totalText"
        } else {
            "$downloadedText / $totalText"
        }
    } else {
        if (artistName != null) {
            "$artistName • ${task.errorMessage ?: task.status.name}"
        } else {
            task.errorMessage ?: task.status.name
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.paddingSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titleText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = statusSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(Dimens.paddingSmall))

        DownloadStateIndicator(
            status = task.status,
            isQueuePaused = isQueuePaused,
            onDownloadClick = onCancelClick,
            onRemoveClick = onCancelClick
        )
    }
}
