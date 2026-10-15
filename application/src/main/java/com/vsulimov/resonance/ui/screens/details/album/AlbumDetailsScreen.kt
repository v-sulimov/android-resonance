package com.vsulimov.resonance.ui.screens.details.album

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.vsulimov.libsubsonic.data.response.browsing.Album
import com.vsulimov.libsubsonic.data.response.browsing.Child
import com.vsulimov.resonance.R
import com.vsulimov.resonance.core.Constants
import com.vsulimov.resonance.core.ui.CoverImage
import com.vsulimov.resonance.core.ui.getAppContainer
import com.vsulimov.resonance.core.utils.DownloadUtils
import com.vsulimov.resonance.core.utils.DurationFormatter
import com.vsulimov.resonance.core.utils.sanitizeForUi
import com.vsulimov.resonance.data.db.DownloadStatus
import com.vsulimov.resonance.ui.components.AlbumOptionsBottomSheet
import com.vsulimov.resonance.ui.components.ErrorState
import com.vsulimov.resonance.ui.components.LoadingState
import com.vsulimov.resonance.ui.components.RemoveDownloadDialog
import com.vsulimov.resonance.ui.components.SongOptionsBottomSheet
import com.vsulimov.resonance.ui.theme.Dimens
import kotlinx.coroutines.launch

/**
 * Stateful entry screen displaying detailed information about an album.
 *
 * @param albumId Unique identifier for the target album.
 * @param innerPadding Outer scaffold content padding.
 * @param onBackClick Callback triggered when user taps back navigation button.
 * @param modifier The modifier to be applied to the layout.
 * @param onArtistClick Callback triggered when artist name is clicked to view artist details.
 * @param viewModel ViewModel holding and managing state for this album.
 */
@Composable
fun AlbumDetailsScreen(
    albumId: String,
    innerPadding: PaddingValues,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onArtistClick: (String) -> Unit = {},
    viewModel: AlbumDetailsViewModel = viewModel(
        key = albumId,
        factory = AlbumDetailsViewModel.provideFactory(albumId)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val appContainer = getAppContainer()
    val playerController = appContainer.playerController
    val favoritesRepository = appContainer.favoritesRepository

    val starredSongIds by favoritesRepository.starredSongIds.collectAsStateWithLifecycle()
    val starredAlbumIds by favoritesRepository.starredAlbumIds.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(uiState.album) {
        val album = uiState.album
        if (album != null && !album.starred.isNullOrBlank()) {
            favoritesRepository.registerStarredAlbum(album.id)
        }
        album?.songs?.forEach { song ->
            if (!song.starred.isNullOrBlank()) {
                favoritesRepository.registerStarredSong(song.id)
            }
        }
    }

    AlbumDetailsContent(
        uiState = uiState,
        innerPadding = innerPadding,
        onBackClick = onBackClick,
        onArtistClick = onArtistClick,
        onPlayQueue = { songs, index -> playerController.playQueue(songs, index) },
        onPlayNext = playerController::playNext,
        onAddToQueue = playerController::addToQueue,
        starredSongIds = starredSongIds,
        starredAlbumIds = starredAlbumIds,
        onToggleSongFavorite = { song ->
            coroutineScope.launch {
                favoritesRepository.toggleSongFavorite(song.id)
            }
        },
        onToggleAlbumFavorite = { album ->
            coroutineScope.launch {
                favoritesRepository.toggleAlbumFavorite(album.id)
            }
        },
        onRetryClick = viewModel::loadAlbumDetails,
        modifier = modifier
    )
}

/**
 * Stateless content layout for displaying album header artwork, tracklist, and play controls.
 *
 * @param uiState Current UI state containing album metadata and loading status.
 * @param innerPadding Outer scaffold content padding.
 * @param onBackClick Callback triggered when user taps back navigation button.
 * @param onArtistClick Callback triggered when artist name is clicked.
 * @param onPlayQueue Callback triggered to play songs queue starting at index.
 * @param onPlayNext Callback triggered to queue song next.
 * @param onAddToQueue Callback triggered to append song to queue.
 * @param onRetryClick Callback triggered on error retry button tap.
 * @param modifier The modifier to be applied to the layout.
 * @param starredSongIds Set of starred song IDs.
 * @param starredAlbumIds Set of starred album IDs.
 * @param onToggleSongFavorite Callback triggered when user toggles song favorite status.
 * @param onToggleAlbumFavorite Callback triggered when user toggles album favorite status.
 */
@Composable
fun AlbumDetailsContent(
    uiState: AlbumDetailsUiState,
    innerPadding: PaddingValues,
    onBackClick: () -> Unit,
    onArtistClick: (String) -> Unit,
    onPlayQueue: (List<Child>, Int) -> Unit,
    onPlayNext: (Child) -> Unit,
    onAddToQueue: (Child) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
    starredSongIds: Set<String> = emptySet(),
    starredAlbumIds: Set<String> = emptySet(),
    onToggleSongFavorite: (Child) -> Unit = {},
    onToggleAlbumFavorite: (Album) -> Unit = {}
) {
    val title = uiState.album?.name?.sanitizeForUi(stringResource(R.string.album_details_title))
        ?: stringResource(R.string.album_details_title)
    var selectedSongForOptions by remember { mutableStateOf<Child?>(null) }
    var showAlbumOptions by remember { mutableStateOf(false) }
    var pendingRemoveItem by remember { mutableStateOf<Pair<String, String>?>(null) }

    val appContainer = getAppContainer()
    val coroutineScope = rememberCoroutineScope()
    val downloadTasks by appContainer.downloadRepository.observeAllTasksSimplified().collectAsStateWithLifecycle(
        initialValue = emptyList()
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            val album = uiState.album
            val isAlbumFavorite = album != null && starredAlbumIds.contains(album.id)

            AlbumDetailsTopBar(
                title = title,
                onBackClick = onBackClick,
                isFavorite = isAlbumFavorite,
                onToggleFavorite = { album?.let(onToggleAlbumFavorite) },
                onMoreClick = { showAlbumOptions = true }
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

            uiState.album != null -> {
                val album = uiState.album
                val safeDrawingInsets = WindowInsets.safeDrawing.asPaddingValues()

                val albumStatus = DownloadUtils.resolveCollectionDownloadStatus(
                    songIds = album.songs.map { it.id },
                    downloadTasks = downloadTasks,
                    storageManager = appContainer.offlineStorageManager,
                    expectedTotalCount = album.songCount ?: album.songs.size
                )
                val hasAlbumDownloadedSongs = remember(album, downloadTasks) {
                    DownloadUtils.hasDownloadedOrActiveSongs(
                        songIds = album.songs.map { it.id },
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
                        AlbumHeader(
                            album = album,
                            downloadStatus = albumStatus,
                            onArtistClick = onArtistClick,
                            onPlayAllClick = { onPlayQueue(album.songs, 0) },
                            onShuffleClick = { onPlayQueue(album.songs.shuffled(), 0) },
                            modifier = Modifier.padding(horizontal = Dimens.paddingMedium)
                        )
                    }

                    if (album.songs.isNotEmpty()) {
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

                        item(key = "tracklist_card") {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Dimens.paddingMedium),
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                )
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    album.songs.forEachIndexed { index, song ->
                                        com.vsulimov.resonance.ui.components.SongMediaListItem(
                                            song = song,
                                            title = song.title.sanitizeForUi(stringResource(R.string.unknown_title)),
                                            subtitle = song.artist.takeIf { it != album.artist }
                                                ?.sanitizeForUi(stringResource(R.string.unknown_artist)),
                                            coverArtId = null,
                                            placeholderIcon = null,
                                            showArtwork = false,
                                            onClick = { onPlayQueue(album.songs, index) },
                                            leadingContent = {
                                                Box(
                                                    modifier = Modifier
                                                        .size(Dimens.trackNumberBoxSize)
                                                        .background(
                                                            color = MaterialTheme.colorScheme.primaryContainer.copy(
                                                                alpha = 0.4f
                                                            ),
                                                            shape = MaterialTheme.shapes.small
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.MusicNote,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(Dimens.iconMedium)
                                                    )
                                                }
                                            },
                                            trailingContent = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = DurationFormatter.formatSeconds(song.duration),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    IconButton(onClick = { selectedSongForOptions = song }) {
                                                        Icon(
                                                            imageVector = Icons.Default.MoreVert,
                                                            contentDescription = stringResource(
                                                                R.string.cd_song_options
                                                            ),
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        )

                                        if (index < album.songs.lastIndex) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = Dimens.paddingMedium),
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (showAlbumOptions) {
                    AlbumOptionsBottomSheet(
                        album = album,
                        onDismiss = { showAlbumOptions = false },
                        isFavorite = starredAlbumIds.contains(album.id),
                        hasDownloadedSongs = hasAlbumDownloadedSongs,
                        downloadStatus = albumStatus,
                        onDownloadClick = {
                            coroutineScope.launch { appContainer.downloadRepository.enqueueAlbum(album.id) }
                        },
                        onRemoveDownloadClick = {
                            pendingRemoveItem = Pair("album_${album.id}", album.name)
                        },
                        onToggleFavorite = {
                            onToggleAlbumFavorite(album)
                        },
                        onGoToArtist = { artistId ->
                            onArtistClick(artistId)
                        }
                    )
                }
            }
        }
    }

    selectedSongForOptions?.let { song ->
        val isSongFavorite = starredSongIds.contains(song.id)
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
            isFavorite = isSongFavorite,
            downloadStatus = songStatus,
            onDownloadClick = {
                coroutineScope.launch { appContainer.downloadRepository.enqueueSong(song) }
            },
            onRemoveDownloadClick = {
                pendingRemoveItem = Pair("song_${song.id}", song.title)
            },
            onToggleFavorite = { onToggleSongFavorite(song) },
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
                if (id.startsWith("album_")) {
                    val albumId = id.removePrefix("album_")
                    coroutineScope.launch { appContainer.downloadRepository.deleteDownloadedAlbum(albumId) }
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
 * Top app bar component for Album Details screen with title, back navigation, favorite action, and options menu.
 *
 * @param title Resolved screen or album title string.
 * @param onBackClick Callback triggered when user taps back button.
 * @param modifier The modifier to be applied to the layout.
 * @param isFavorite True if the album is marked as favorite.
 * @param onToggleFavorite Callback triggered when user taps favorite icon.
 * @param onMoreClick Callback triggered when user taps overflow menu icon.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlbumDetailsTopBar(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
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
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = stringResource(
                        if (isFavorite) R.string.cd_remove_from_favorites else R.string.cd_add_to_favorites
                    ),
                    tint = if (isFavorite) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
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
 * Header section component displaying album artwork, title, artist link, year/tracks metadata, and play buttons.
 *
 * @param album Target album data object.
 * @param onArtistClick Callback triggered on artist name tap.
 * @param onPlayAllClick Callback triggered on Play button tap.
 * @param onShuffleClick Callback triggered on Shuffle button tap.
 * @param modifier The modifier to be applied to the layout.
 * @param downloadStatus Current download status of the album collection.
 */
@Composable
private fun AlbumHeader(
    album: Album,
    onArtistClick: (String) -> Unit,
    onPlayAllClick: () -> Unit,
    onShuffleClick: () -> Unit,
    modifier: Modifier = Modifier,
    downloadStatus: DownloadStatus = DownloadStatus.NOT_DOWNLOADED
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val artistName = album.artist.sanitizeForUi(stringResource(R.string.unknown_artist))
    val artistId = album.artistId
    val isArtistClickable = !artistId.isNullOrBlank()

    val yearText = album.year?.takeIf { it > 0 }?.toString()
    val songCountText = stringResource(R.string.song_count_format, album.songCount ?: 0)
    val infoText = if (yearText != null) {
        stringResource(R.string.bullet_separated_format, yearText, songCountText)
    } else {
        songCountText
    }

    val coverArtId = album.coverArt

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
                            imageVector = Icons.Default.Album,
                            contentDescription = stringResource(R.string.cd_album_cover),
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
                    text = album.name.sanitizeForUi(stringResource(R.string.unknown_album)),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(Dimens.paddingSmall))

                Text(
                    text = artistName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isArtistClickable) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable(enabled = isArtistClickable) {
                        artistId?.let(onArtistClick)
                    }
                )

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
                    horizontalArrangement = Arrangement.spacedBy(Dimens.paddingSmall),
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
                            imageVector = Icons.Default.Album,
                            contentDescription = stringResource(R.string.cd_album_cover),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(Dimens.iconExtraLarge)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.paddingMedium))

            Text(
                text = album.name.sanitizeForUi(stringResource(R.string.unknown_album)),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Dimens.paddingSmall))

            Text(
                text = artistName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isArtistClickable) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = isArtistClickable) {
                        artistId?.let(onArtistClick)
                    }
            )

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
                horizontalArrangement = Arrangement.spacedBy(Dimens.paddingSmall),
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
