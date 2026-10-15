package com.vsulimov.resonance.ui.screens.details.playlist

import android.content.res.Configuration
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsulimov.libsubsonic.data.response.browsing.Child
import com.vsulimov.libsubsonic.data.response.playlists.Playlist
import com.vsulimov.resonance.R
import com.vsulimov.resonance.core.Constants
import com.vsulimov.resonance.core.ui.CoverImage
import com.vsulimov.resonance.core.ui.getAppContainer
import com.vsulimov.resonance.core.utils.DownloadUtils
import com.vsulimov.resonance.core.utils.DurationFormatter
import com.vsulimov.resonance.core.utils.sanitizeForUi
import com.vsulimov.resonance.data.db.DownloadStatus
import com.vsulimov.resonance.ui.components.ErrorState
import com.vsulimov.resonance.ui.components.LoadingState
import com.vsulimov.resonance.ui.components.PlaylistOptionsBottomSheet
import com.vsulimov.resonance.ui.components.RemoveDownloadDialog
import com.vsulimov.resonance.ui.components.SongOptionsBottomSheet
import com.vsulimov.resonance.ui.theme.Dimens
import kotlinx.coroutines.launch

/**
 * Stateful entry screen displaying detailed information about a playlist and its track entries.
 *
 * @param playlistId Unique identifier for the target playlist.
 * @param innerPadding Outer scaffold content padding.
 * @param onBackClick Callback triggered when user taps back navigation button.
 * @param modifier The modifier to be applied to the layout.
 * @param onAlbumClick Callback triggered when album link is clicked.
 * @param onArtistClick Callback triggered when artist link is clicked.
 * @param viewModel ViewModel holding and managing state for this playlist.
 */
@Composable
fun PlaylistDetailsScreen(
    playlistId: String,
    innerPadding: PaddingValues,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onAlbumClick: (String) -> Unit = {},
    onArtistClick: (String) -> Unit = {},
    viewModel: PlaylistDetailsViewModel = viewModel(
        key = playlistId,
        factory = PlaylistDetailsViewModel.provideFactory(playlistId)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playerController = getAppContainer().playerController

    PlaylistDetailsContent(
        uiState = uiState,
        innerPadding = innerPadding,
        onBackClick = onBackClick,
        onPlayQueue = { songs, index -> playerController.playQueue(songs, index) },
        onPlayNext = playerController::playNext,
        onAddToQueue = playerController::addToQueue,
        onAlbumClick = onAlbumClick,
        onArtistClick = onArtistClick,
        onRetryClick = viewModel::loadPlaylistDetails,
        modifier = modifier
    )
}

/**
 * Stateless content layout for displaying playlist header artwork, track entries, and play actions.
 *
 * @param uiState Current UI state containing playlist metadata and track entries.
 * @param innerPadding Outer scaffold content padding.
 * @param onBackClick Callback triggered when user taps back navigation button.
 * @param onPlayQueue Callback triggered to play songs queue starting at index.
 * @param onPlayNext Callback triggered to queue song next.
 * @param onAddToQueue Callback triggered to append song to queue.
 * @param onAlbumClick Callback triggered when album link is clicked.
 * @param onArtistClick Callback triggered when artist link is clicked.
 * @param onRetryClick Callback triggered on error retry button tap.
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
fun PlaylistDetailsContent(
    uiState: PlaylistDetailsUiState,
    innerPadding: PaddingValues,
    onBackClick: () -> Unit,
    onPlayQueue: (List<Child>, Int) -> Unit,
    onPlayNext: (Child) -> Unit,
    onAddToQueue: (Child) -> Unit,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = uiState.playlist?.name?.sanitizeForUi(stringResource(R.string.playlist_details_title))
        ?: stringResource(R.string.playlist_details_title)
    var selectedSongForOptions by remember { mutableStateOf<Child?>(null) }
    var showPlaylistOptions by remember { mutableStateOf(false) }
    var pendingRemoveItem by remember { mutableStateOf<Pair<String, String>?>(null) }

    val appContainer = getAppContainer()
    val coroutineScope = rememberCoroutineScope()
    val downloadTasks by appContainer.downloadRepository.observeAllTasksSimplified().collectAsStateWithLifecycle(
        initialValue = emptyList()
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            PlaylistDetailsTopBar(
                title = title,
                onBackClick = onBackClick,
                onMoreClick = { showPlaylistOptions = true }
            )
        }
    ) { scaffoldPadding ->
        when {
            uiState.isLoading -> {
                LoadingState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(scaffoldPadding)
                )
            }

            uiState.error != null -> {
                ErrorState(
                    message = uiState.error.asString(),
                    onRetryClick = onRetryClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(scaffoldPadding)
                )
            }

            uiState.playlist != null -> {
                val playlist = uiState.playlist
                val safeDrawingInsets = WindowInsets.safeDrawing.asPaddingValues()

                val playlistDownloadStatus = DownloadUtils.resolveCollectionDownloadStatus(
                    songIds = playlist.entries.map { it.id },
                    downloadTasks = downloadTasks,
                    storageManager = appContainer.offlineStorageManager,
                    expectedTotalCount = playlist.songCount
                )
                val hasPlaylistDownloadedSongs = remember(playlist, downloadTasks) {
                    DownloadUtils.hasDownloadedOrActiveSongs(
                        songIds = playlist.entries.map { it.id },
                        downloadTasks = downloadTasks,
                        storageManager = appContainer.offlineStorageManager
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = safeDrawingInsets.calculateLeftPadding(LayoutDirection.Ltr),
                        top = scaffoldPadding.calculateTopPadding() + Dimens.paddingMedium,
                        end = safeDrawingInsets.calculateRightPadding(LayoutDirection.Ltr),
                        bottom = innerPadding.calculateBottomPadding() + Dimens.paddingLarge
                    ),
                    verticalArrangement = Arrangement.spacedBy(Dimens.paddingSmall)
                ) {
                    item {
                        PlaylistHeader(
                            playlist = playlist,
                            downloadStatus = playlistDownloadStatus,
                            onPlayAllClick = { onPlayQueue(playlist.entries, 0) },
                            onShuffleClick = { onPlayQueue(playlist.entries.shuffled(), 0) },
                            modifier = Modifier.padding(horizontal = Dimens.paddingMedium)
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(Dimens.paddingMedium))
                        Text(
                            text = stringResource(R.string.section_tracks),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(
                                horizontal = Dimens.paddingMedium,
                                vertical = Dimens.paddingSmall
                            )
                        )
                    }

                    itemsIndexed(
                        items = playlist.entries,
                        key = { index, (id) -> "entry_${index}_$id" }
                    ) { index, song ->
                        val artistName = song.artist.sanitizeForUi(stringResource(R.string.unknown_artist))
                        val albumName = song.album.sanitizeForUi(stringResource(R.string.unknown_album))
                        val subtitleText = stringResource(R.string.bullet_separated_format, artistName, albumName)

                        com.vsulimov.resonance.ui.components.SongMediaListItem(
                            song = song,
                            title = song.title.sanitizeForUi(stringResource(R.string.unknown_title)),
                            subtitle = subtitleText,
                            coverArtId = song.coverArt ?: playlist.coverArt,
                            placeholderIcon = Icons.Default.MusicNote,
                            onClick = { onPlayQueue(playlist.entries, index) },
                            trailingContent = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = DurationFormatter.formatSeconds(song.duration),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    IconButton(onClick = { selectedSongForOptions = song }) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = stringResource(R.string.cd_song_options),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        )
                    }
                }

                if (showPlaylistOptions) {
                    PlaylistOptionsBottomSheet(
                        playlist = playlist,
                        onDismiss = { showPlaylistOptions = false },
                        hasDownloadedSongs = hasPlaylistDownloadedSongs,
                        downloadStatus = playlistDownloadStatus,
                        onDownloadClick = {
                            coroutineScope.launch { appContainer.downloadRepository.enqueuePlaylist(playlist.id) }
                        },
                        onRemoveDownloadClick = {
                            pendingRemoveItem = Pair("playlist_${playlist.id}", playlist.name)
                        }
                    )
                }
            }
        }
    }

    selectedSongForOptions?.let { song ->
        val (songStatus, _) = DownloadUtils.resolveSongDownloadState(
            songId = song.id,
            suffix = song.suffix,
            downloadTasks = downloadTasks,
            storageManager = appContainer.offlineStorageManager
        )

        SongOptionsBottomSheet(
            song = song,
            onDismiss = { selectedSongForOptions = null },
            onPlayNext = { onPlayNext(song) },
            onAddToQueue = { onAddToQueue(song) },
            downloadStatus = songStatus,
            onDownloadClick = {
                coroutineScope.launch { appContainer.downloadRepository.enqueueSong(song) }
            },
            onRemoveDownloadClick = {
                pendingRemoveItem = Pair("song_${song.id}", song.title)
            },
            onGoToAlbum = { albumId ->
                selectedSongForOptions = null
                onAlbumClick(albumId)
            },
            onGoToArtist = { artistId ->
                selectedSongForOptions = null
                onArtistClick(artistId)
            }
        )
    }

    pendingRemoveItem?.let { (id, itemName) ->
        RemoveDownloadDialog(
            itemName = itemName,
            onConfirm = {
                if (id.startsWith("playlist_")) {
                    val playlistId = id.removePrefix("playlist_")
                    coroutineScope.launch { appContainer.downloadRepository.deleteDownloadedPlaylist(playlistId) }
                } else if (id.startsWith("song_")) {
                    val songId = id.removePrefix("song_")
                    coroutineScope.launch { appContainer.downloadRepository.deleteDownloadedSong(songId) }
                }
                pendingRemoveItem = null
            },
            onDismiss = { pendingRemoveItem = null }
        )
    }
}

/**
 * Top app bar component for Playlist Details screen with title, back navigation, and options menu.
 *
 * @param title Resolved screen or playlist title string.
 * @param onBackClick Callback triggered when user taps back button.
 * @param modifier The modifier to be applied to the layout.
 * @param onMoreClick Callback triggered when user taps overflow menu icon.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistDetailsTopBar(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onMoreClick: () -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
            IconButton(onClick = onMoreClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.cd_song_options),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
    )
}

/**
 * Header section component displaying playlist cover art, title, comment, track count, duration, and play/shuffle actions.
 *
 * @param playlist Target playlist data object.
 * @param downloadStatus Current download status of playlist collection.
 * @param onPlayAllClick Callback triggered on Play button tap.
 * @param onShuffleClick Callback triggered on Shuffle button tap.
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
private fun PlaylistHeader(
    playlist: Playlist,
    onPlayAllClick: () -> Unit,
    onShuffleClick: () -> Unit,
    modifier: Modifier = Modifier,
    downloadStatus: DownloadStatus = DownloadStatus.NOT_DOWNLOADED
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val comment = playlist.comment
    val songCountText = stringResource(R.string.song_count_format, playlist.songCount)
    val durationText = DurationFormatter.formatSeconds(playlist.duration)
    val infoText = stringResource(R.string.bullet_separated_format, songCountText, durationText)
    val coverArtId = playlist.coverArt

    if (isLandscape) {
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                modifier = Modifier.size(Dimens.artworkSizeDetailsHeaderLandscape),
                shape = MaterialTheme.shapes.medium
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
                            imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                            contentDescription = stringResource(R.string.cd_playlist_cover),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(Dimens.iconExtraLarge)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(Dimens.paddingMedium))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = playlist.name.sanitizeForUi(stringResource(R.string.unknown_title)),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (!comment.isNullOrBlank()) {
                    Text(
                        text = comment,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = infoText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (downloadStatus == DownloadStatus.DOWNLOADED) {
                    Spacer(modifier = Modifier.height(Dimens.paddingExtraSmall))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = stringResource(R.string.cd_downloaded),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(Dimens.iconExtraSmall)
                        )
                        Spacer(modifier = Modifier.width(Dimens.paddingExtraSmall))
                        Text(
                            text = stringResource(R.string.cd_downloaded),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.paddingSmall))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.paddingSmall)
                ) {
                    Button(
                        onClick = onPlayAllClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = stringResource(R.string.cd_play_button)
                        )
                        Spacer(modifier = Modifier.width(Dimens.paddingSmall))
                        Text(text = stringResource(R.string.btn_play))
                    }

                    OutlinedButton(
                        onClick = onShuffleClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = stringResource(R.string.cd_shuffle_button)
                        )
                        Spacer(modifier = Modifier.width(Dimens.paddingSmall))
                        Text(text = stringResource(R.string.btn_shuffle))
                    }
                }
            }
        }
    } else {
        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier
                    .sizeIn(
                        maxWidth = Dimens.artworkSizeDetailsHeaderPortraitMax,
                        maxHeight = Dimens.artworkSizeDetailsHeaderPortraitMax
                    )
                    .aspectRatio(1f),
                shape = MaterialTheme.shapes.large
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
                            imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                            contentDescription = stringResource(R.string.cd_playlist_cover),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(Dimens.iconExtraLarge)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.paddingMedium))

            Text(
                text = playlist.name.sanitizeForUi(stringResource(R.string.unknown_title)),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            if (!comment.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(Dimens.paddingSmall))
                Text(
                    text = comment,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(Dimens.paddingSmall))

            Text(
                text = infoText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (downloadStatus == DownloadStatus.DOWNLOADED) {
                Spacer(modifier = Modifier.height(Dimens.paddingExtraSmall))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = stringResource(R.string.cd_downloaded),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(Dimens.iconExtraSmall)
                    )
                    Spacer(modifier = Modifier.width(Dimens.paddingExtraSmall))
                    Text(
                        text = stringResource(R.string.cd_downloaded),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.paddingMedium))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onPlayAllClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.cd_play_button)
                    )
                    Spacer(modifier = Modifier.width(Dimens.paddingSmall))
                    Text(text = stringResource(R.string.btn_play))
                }

                Spacer(modifier = Modifier.width(Dimens.paddingMedium))

                OutlinedButton(
                    onClick = onShuffleClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = stringResource(R.string.cd_shuffle_button)
                    )
                    Spacer(modifier = Modifier.width(Dimens.paddingSmall))
                    Text(text = stringResource(R.string.btn_shuffle))
                }
            }
        }
    }
}
