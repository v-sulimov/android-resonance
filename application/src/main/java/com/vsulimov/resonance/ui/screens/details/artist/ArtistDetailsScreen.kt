package com.vsulimov.resonance.ui.screens.details.artist

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.vsulimov.libsubsonic.data.response.browsing.Artist
import com.vsulimov.resonance.R
import com.vsulimov.resonance.core.Constants
import com.vsulimov.resonance.core.ui.CoverImage
import com.vsulimov.resonance.core.ui.getAppContainer
import com.vsulimov.resonance.core.utils.DownloadUtils
import com.vsulimov.resonance.core.utils.sanitizeForUi
import com.vsulimov.resonance.data.db.DownloadStatus
import com.vsulimov.resonance.ui.components.ArtistOptionsBottomSheet
import com.vsulimov.resonance.ui.components.ErrorState
import com.vsulimov.resonance.ui.components.LoadingState
import com.vsulimov.resonance.ui.components.MediaGridItem
import com.vsulimov.resonance.ui.components.RemoveDownloadDialog
import com.vsulimov.resonance.ui.theme.Dimens
import kotlinx.coroutines.launch

/**
 * Stateful entry screen displaying detailed information about an artist and their discography.
 *
 * @param artistId Unique identifier for the target artist.
 * @param innerPadding Outer scaffold content padding.
 * @param onBackClick Callback triggered when user taps back navigation button.
 * @param modifier The modifier to be applied to the layout.
 * @param onAlbumClick Callback triggered when an album grid item is clicked.
 * @param viewModel ViewModel holding and managing state for this artist.
 */
@Composable
fun ArtistDetailsScreen(
    artistId: String,
    innerPadding: PaddingValues,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onAlbumClick: (String) -> Unit = {},
    viewModel: ArtistDetailsViewModel = viewModel(
        key = artistId,
        factory = ArtistDetailsViewModel.provideFactory(artistId)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val appContainer = getAppContainer()
    val favoritesRepository = appContainer.favoritesRepository

    val starredArtistIds by favoritesRepository.starredArtistIds.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(uiState.artist) {
        val artist = uiState.artist
        if (artist != null && !artist.starred.isNullOrBlank()) {
            favoritesRepository.registerStarredArtist(artist.id)
        }
    }

    ArtistDetailsContent(
        uiState = uiState,
        innerPadding = innerPadding,
        onBackClick = onBackClick,
        onAlbumClick = onAlbumClick,
        starredArtistIds = starredArtistIds,
        onToggleArtistFavorite = { artist ->
            coroutineScope.launch {
                favoritesRepository.toggleArtistFavorite(artist.id)
            }
        },
        onRetryClick = viewModel::loadArtistDetails,
        modifier = modifier
    )
}

/**
 * Stateless content layout for displaying artist avatar profile, album discography grid, and loading/error states.
 *
 * @param uiState Current UI state containing artist data and loading status.
 * @param innerPadding Outer scaffold content padding.
 * @param onBackClick Callback triggered when user taps back navigation button.
 * @param onAlbumClick Callback triggered when an album item is clicked.
 * @param starredArtistIds Set of starred artist IDs.
 * @param onToggleArtistFavorite Callback triggered when user toggles artist favorite status.
 * @param onRetryClick Callback triggered on error retry button tap.
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
fun ArtistDetailsContent(
    uiState: ArtistDetailsUiState,
    innerPadding: PaddingValues,
    onBackClick: () -> Unit,
    onAlbumClick: (String) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
    starredArtistIds: Set<String> = emptySet(),
    onToggleArtistFavorite: (Artist) -> Unit = {}
) {
    val title = uiState.artist?.name?.sanitizeForUi(stringResource(R.string.artist_details_title))
        ?: stringResource(R.string.artist_details_title)

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val artist = uiState.artist
    val isArtistFavorite = artist != null && starredArtistIds.contains(artist.id)

    val appContainer = getAppContainer()
    val coroutineScope = rememberCoroutineScope()
    val downloadTasks by appContainer.downloadRepository.observeAllTasksSimplified().collectAsStateWithLifecycle(
        initialValue = emptyList()
    )

    var showArtistOptions by remember { mutableStateOf(false) }
    var pendingRemoveArtist by remember { mutableStateOf<Artist?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ArtistDetailsTopBar(
                title = title,
                onBackClick = onBackClick,
                isFavorite = isArtistFavorite,
                onToggleFavorite = { artist?.let(onToggleArtistFavorite) },
                onMoreClick = { showArtistOptions = true }
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

            uiState.artist != null -> {
                val currentArtist = uiState.artist
                val safeDrawingInsets = WindowInsets.safeDrawing.asPaddingValues()

                val allArtistSongs by appContainer.database.songDao().observeSongsByArtistId(
                    currentArtist.id
                ).collectAsStateWithLifecycle(
                    initialValue = emptyList()
                )

                val isQueuePaused by appContainer.settingsRepository
                    .isDownloadQueuePausedState.collectAsStateWithLifecycle()

                val albumStatusMap = remember(currentArtist.albums, allArtistSongs, downloadTasks) {
                    currentArtist.albums.associate { album ->
                        val albumSongIds = allArtistSongs.filter { it.albumId == album.id }.map { it.id }
                        val queuedOrActiveForAlbum = downloadTasks.filter { task ->
                            task.songId in albumSongIds ||
                                allArtistSongs.any { it.id == task.songId && it.albumId == album.id }
                        }
                        val status = if (queuedOrActiveForAlbum.any { it.status == DownloadStatus.DOWNLOADING }) {
                            DownloadStatus.DOWNLOADING
                        } else if (queuedOrActiveForAlbum.any { it.status == DownloadStatus.QUEUED }) {
                            DownloadStatus.QUEUED
                        } else {
                            DownloadUtils.resolveCollectionDownloadStatus(
                                songIds = albumSongIds,
                                downloadTasks = downloadTasks,
                                storageManager = appContainer.offlineStorageManager,
                                expectedTotalCount = album.songCount ?: 0
                            )
                        }
                        album.id to status
                    }
                }

                val isArtistFullyDownloaded = remember(albumStatusMap, currentArtist.albums) {
                    currentArtist.albums.isNotEmpty() && currentArtist.albums.all { album ->
                        albumStatusMap[album.id] == DownloadStatus.DOWNLOADED
                    }
                }

                val artistDownloadStatus = if (isArtistFullyDownloaded) {
                    DownloadStatus.DOWNLOADED
                } else {
                    DownloadStatus.NOT_DOWNLOADED
                }

                val hasArtistDownloadedSongs = remember(allArtistSongs, downloadTasks) {
                    DownloadUtils.hasDownloadedOrActiveSongs(
                        songIds = allArtistSongs.map { it.id },
                        downloadTasks = downloadTasks,
                        storageManager = appContainer.offlineStorageManager
                    )
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (isLandscape) 4 else 2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = safeDrawingInsets.calculateLeftPadding(LayoutDirection.Ltr) + Dimens.paddingMedium,
                        top = scaffoldPadding.calculateTopPadding() + Dimens.paddingMedium,
                        end = safeDrawingInsets.calculateRightPadding(LayoutDirection.Ltr) + Dimens.paddingMedium,
                        bottom = innerPadding.calculateBottomPadding() + Dimens.paddingLarge
                    ),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.paddingMedium),
                    verticalArrangement = Arrangement.spacedBy(Dimens.paddingMedium)
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        ArtistHeader(
                            artist = currentArtist,
                            downloadStatus = artistDownloadStatus
                        )
                    }

                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = stringResource(R.string.section_albums),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = Dimens.paddingSmall)
                        )
                    }

                    items(
                        items = currentArtist.albums,
                        key = { (id) -> id }
                    ) { album ->
                        val yearText = album.year?.takeIf { it > 0 }?.toString()
                        val albumStatus = albumStatusMap[album.id] ?: DownloadStatus.NOT_DOWNLOADED

                        MediaGridItem(
                            title = album.name.sanitizeForUi(stringResource(R.string.unknown_album)),
                            subtitle = yearText ?: stringResource(R.string.song_count_format, album.songCount ?: 0),
                            coverArtId = album.coverArt,
                            placeholderIcon = Icons.Default.Album,
                            onClick = { onAlbumClick(album.id) },
                            isDownloaded = albumStatus == DownloadStatus.DOWNLOADED,
                            downloadStatus = albumStatus,
                            isQueuePaused = isQueuePaused
                        )
                    }
                }

                if (showArtistOptions) {
                    ArtistOptionsBottomSheet(
                        artist = currentArtist,
                        onDismiss = { showArtistOptions = false },
                        isFavorite = isArtistFavorite,
                        hasDownloadedSongs = hasArtistDownloadedSongs,
                        downloadStatus = artistDownloadStatus,
                        onDownloadClick = {
                            coroutineScope.launch {
                                appContainer.downloadRepository.enqueueArtist(currentArtist.id)
                            }
                        },
                        onRemoveDownloadClick = {
                            pendingRemoveArtist = currentArtist
                        },
                        onToggleFavorite = {
                            onToggleArtistFavorite(currentArtist)
                        }
                    )
                }
            }
        }
    }

    pendingRemoveArtist?.let { targetArtist ->
        RemoveDownloadDialog(
            itemName = targetArtist.name,
            onConfirm = {
                coroutineScope.launch { appContainer.downloadRepository.deleteDownloadedArtist(targetArtist.id) }
                pendingRemoveArtist = null
            },
            onDismiss = { pendingRemoveArtist = null }
        )
    }
}

/**
 * Top app bar component for Artist Details screen with title, back navigation, favorite action, and options menu.
 *
 * @param title Resolved screen or artist title string.
 * @param onBackClick Callback triggered when user taps back button.
 * @param modifier The modifier to be applied to the layout.
 * @param isFavorite True if the artist is marked as favorite.
 * @param onToggleFavorite Callback triggered when user taps favorite icon.
 * @param onMoreClick Callback triggered when user taps overflow menu icon.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArtistDetailsTopBar(
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
 * Header section component displaying artist avatar image, name, and total album count.
 *
 * @param artist Target artist data object.
 * @param modifier The modifier to be applied to the layout.
 * @param downloadStatus Current download status of artist collection.
 */
@Composable
private fun ArtistHeader(
    artist: Artist,
    modifier: Modifier = Modifier,
    downloadStatus: DownloadStatus = DownloadStatus.NOT_DOWNLOADED
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val coverId = artist.coverArt

    if (isLandscape) {
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                modifier = Modifier.size(Dimens.avatarSizeDetailsHeaderLandscape),
                shape = CircleShape
            ) {
                if (coverId != null) {
                    CoverImage(
                        coverArtId = coverId,
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
                            imageVector = Icons.Default.Person,
                            contentDescription = stringResource(R.string.cd_artist_avatar),
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
                    text = artist.name.sanitizeForUi(stringResource(R.string.unknown_artist)),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(Dimens.paddingSmall))

                Text(
                    text = stringResource(R.string.album_count_format, artist.albumCount),
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
                        maxWidth = Dimens.avatarSizeDetailsHeaderPortraitMax,
                        maxHeight = Dimens.avatarSizeDetailsHeaderPortraitMax
                    )
                    .aspectRatio(1f),
                shape = CircleShape
            ) {
                if (coverId != null) {
                    CoverImage(
                        coverArtId = coverId,
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
                            imageVector = Icons.Default.Person,
                            contentDescription = stringResource(R.string.cd_artist_avatar),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(Dimens.iconExtraLarge)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.paddingMedium))

            Text(
                text = artist.name.sanitizeForUi(stringResource(R.string.unknown_artist)),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(Dimens.paddingSmall))

            Text(
                text = stringResource(R.string.album_count_format, artist.albumCount),
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
        }
    }
}
