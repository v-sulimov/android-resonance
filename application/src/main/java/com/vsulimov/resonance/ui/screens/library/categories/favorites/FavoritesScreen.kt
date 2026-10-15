package com.vsulimov.resonance.ui.screens.library.categories.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsulimov.libsubsonic.data.response.browsing.Child
import com.vsulimov.resonance.R
import com.vsulimov.resonance.core.ui.getAppContainer
import com.vsulimov.resonance.core.utils.DownloadUtils
import com.vsulimov.resonance.core.utils.DurationFormatter
import com.vsulimov.resonance.core.utils.sanitizeForUi
import com.vsulimov.resonance.data.db.DownloadStatus
import com.vsulimov.resonance.ui.components.CategoryHeaderActions
import com.vsulimov.resonance.ui.components.CategoryTopBar
import com.vsulimov.resonance.ui.components.EmptyState
import com.vsulimov.resonance.ui.components.ErrorState
import com.vsulimov.resonance.ui.components.LoadingState
import com.vsulimov.resonance.ui.components.MediaListItem
import com.vsulimov.resonance.ui.components.RemoveDownloadDialog
import com.vsulimov.resonance.ui.components.ScrollToTopFab
import com.vsulimov.resonance.ui.components.SongOptionsBottomSheet
import com.vsulimov.resonance.ui.components.SortBottomSheet
import com.vsulimov.resonance.ui.theme.Dimens
import kotlinx.coroutines.launch

private const val SCROLL_TO_TOP_THRESHOLD = 3

/**
 * Stateful screen composable for the Favorites Category view.
 *
 * @param innerPadding Padding values provided by the root Scaffold.
 * @param onBackClick Callback triggered when navigating back.
 * @param modifier The modifier to be applied to the layout.
 * @param onAlbumClick Callback triggered when an album item is clicked.
 * @param onArtistClick Callback triggered when an artist item is clicked.
 * @param viewModel ViewModel holding and managing the screen state.
 */
@Composable
fun FavoritesScreen(
    innerPadding: PaddingValues,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onAlbumClick: (String) -> Unit = {},
    onArtistClick: (String) -> Unit = {},
    viewModel: FavoritesViewModel = viewModel(factory = FavoritesViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val appContainer = getAppContainer()
    val playerController = appContainer.playerController
    val favoritesRepository = appContainer.favoritesRepository

    val starredSongIds by favoritesRepository.starredSongIds.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    FavoritesContent(
        uiState = uiState,
        innerPadding = innerPadding,
        onBackClick = onBackClick,
        onRetryClick = viewModel::loadFavorites,
        onSearchQueryChange = viewModel::setSearchQuery,
        onSearchToggle = viewModel::toggleSearch,
        onSortClick = viewModel::toggleSortSheet,
        onFilterSelect = viewModel::selectFilter,
        onPlayQueue = { songs, index -> playerController.playQueue(songs, index) },
        onPlayNext = playerController::playNext,
        onAddToQueue = playerController::addToQueue,
        starredSongIds = starredSongIds,
        onToggleSongFavorite = { song ->
            coroutineScope.launch {
                favoritesRepository.toggleSongFavorite(song.id)
                viewModel.loadFavorites()
            }
        },
        onAlbumClick = onAlbumClick,
        onArtistClick = onArtistClick,
        modifier = modifier
    )

    if (uiState.isSortSheetOpen) {
        SortBottomSheet(
            options = FAVORITES_SORT_OPTIONS,
            selectedOption = uiState.selectedSort,
            isAscending = uiState.isSortAscending,
            onOptionSelect = viewModel::setSortOption,
            onAscendingToggle = viewModel::toggleSortDirection,
            onDismissRequest = viewModel::toggleSortSheet
        )
    }
}

/**
 * Stateless content composable displaying starred items categorized with Material 3 Filter Chips.
 *
 * @param uiState Current UI state containing starred lists, selected filter, and status flags.
 * @param innerPadding Padding values provided by the root Scaffold.
 * @param onBackClick Callback triggered when back navigation is requested.
 * @param onRetryClick Callback triggered when retrying data load after error.
 * @param onSearchQueryChange Callback triggered on search text input.
 * @param onSearchToggle Callback triggered to toggle search mode.
 * @param onSortClick Callback triggered to open sort sheet.
 * @param onFilterSelect Callback triggered when a filter chip is selected.
 * @param onPlayQueue Callback triggered to play songs queue starting at index.
 * @param onPlayNext Callback triggered to queue song next.
 * @param onAddToQueue Callback triggered to append song to queue.
 * @param onAlbumClick Callback triggered when a starred album is clicked.
 * @param onArtistClick Callback triggered when a starred artist is clicked.
 * @param modifier The modifier to be applied to the layout.
 * @param starredSongIds Set of starred song IDs.
 * @param onToggleSongFavorite Callback triggered when user toggles favorite status for a song.
 */
@Composable
fun FavoritesContent(
    uiState: FavoritesUiState,
    innerPadding: PaddingValues,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchToggle: () -> Unit,
    onSortClick: () -> Unit,
    onFilterSelect: (FavoritesFilter) -> Unit,
    onPlayQueue: (List<Child>, Int) -> Unit,
    onPlayNext: (Child) -> Unit,
    onAddToQueue: (Child) -> Unit,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    starredSongIds: Set<String> = emptySet(),
    onToggleSongFavorite: (Child) -> Unit = {}
) {
    val safeInsets = WindowInsets.safeDrawing.asPaddingValues()
    val leftInset = safeInsets.calculateLeftPadding(LayoutDirection.Ltr)
    val rightInset = safeInsets.calculateRightPadding(LayoutDirection.Ltr)
    var selectedSongForOptions by remember { mutableStateOf<Child?>(null) }
    var pendingRemoveSong by remember { mutableStateOf<Child?>(null) }

    val appContainer = getAppContainer()
    val coroutineScope = rememberCoroutineScope()
    val downloadTasks by appContainer.downloadRepository.observeAllTasksSimplified().collectAsStateWithLifecycle(
        initialValue = emptyList()
    )

    val activeItemCount = when (uiState.selectedFilter) {
        FavoritesFilter.ALL -> uiState.filteredSongs.size + uiState.filteredAlbums.size + uiState.filteredArtists.size
        FavoritesFilter.SONGS -> uiState.filteredSongs.size
        FavoritesFilter.ALBUMS -> uiState.filteredAlbums.size
        FavoritesFilter.ARTISTS -> uiState.filteredArtists.size
    }

    val listState = rememberLazyListState()

    val isScrollToTopVisible by remember {
        derivedStateOf { listState.firstVisibleItemIndex > SCROLL_TO_TOP_THRESHOLD }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding() + Dimens.paddingSmall
                )
        ) {
            CategoryTopBar(
                title = stringResource(R.string.library_category_favorites_title),
                onBackClick = onBackClick,
                searchQuery = uiState.searchQuery,
                isSearchActive = uiState.isSearchActive,
                onSearchQueryChange = onSearchQueryChange,
                onSearchToggle = onSearchToggle,
                onSortClick = onSortClick,
                modifier = Modifier.padding(
                    start = leftInset,
                    end = rightInset
                )
            )

            when {
                uiState.isSearchActive && uiState.searchQuery.isBlank() -> {
                    EmptyState(
                        icon = Icons.Default.Search,
                        title = stringResource(R.string.search_prompt_favorites_title),
                        description = stringResource(R.string.search_prompt_favorites_desc),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding())
                    )
                }

                uiState.isLoading -> {
                    LoadingState(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding())
                    )
                }

                uiState.error != null -> {
                    ErrorState(
                        message = uiState.error.asString(),
                        onRetryClick = onRetryClick,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding())
                    )
                }

                uiState.starredSongs.isEmpty() &&
                    uiState.starredAlbums.isEmpty() &&
                    uiState.starredArtists.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Default.Favorite,
                        title = stringResource(R.string.empty_favorites_title),
                        description = stringResource(R.string.empty_favorites_desc),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding())
                    )
                }

                else -> {
                    FilterChipsRow(
                        selectedFilter = uiState.selectedFilter,
                        contentPadding = PaddingValues(
                            start = Dimens.paddingMedium + leftInset,
                            end = Dimens.paddingMedium + rightInset
                        ),
                        onFilterSelect = onFilterSelect
                    )

                    if (activeItemCount == 0) {
                        val emptyTitleRes = when (uiState.selectedFilter) {
                            FavoritesFilter.SONGS -> R.string.empty_songs_title
                            FavoritesFilter.ALBUMS -> R.string.empty_albums_title
                            FavoritesFilter.ARTISTS -> R.string.empty_artists_title
                            FavoritesFilter.ALL -> R.string.empty_favorites_title
                        }
                        val emptyDescRes = when (uiState.selectedFilter) {
                            FavoritesFilter.SONGS -> R.string.empty_songs_desc
                            FavoritesFilter.ALBUMS -> R.string.empty_albums_desc
                            FavoritesFilter.ARTISTS -> R.string.empty_artists_desc
                            FavoritesFilter.ALL -> R.string.empty_favorites_desc
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(bottom = innerPadding.calculateBottomPadding()),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyState(
                                icon = Icons.Default.Favorite,
                                title = stringResource(emptyTitleRes),
                                description = stringResource(emptyDescRes)
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                bottom = Dimens.paddingLarge + innerPadding.calculateBottomPadding(),
                                start = leftInset,
                                end = rightInset
                            )
                        ) {
                            item {
                                CategoryHeaderActions(
                                    itemCount = activeItemCount
                                )
                            }

                            val showSongs =
                                (
                                    uiState.selectedFilter == FavoritesFilter.ALL ||
                                        uiState.selectedFilter == FavoritesFilter.SONGS
                                    ) &&
                                    uiState.filteredSongs.isNotEmpty()
                            val showAlbums =
                                (
                                    uiState.selectedFilter == FavoritesFilter.ALL ||
                                        uiState.selectedFilter == FavoritesFilter.ALBUMS
                                    ) &&
                                    uiState.filteredAlbums.isNotEmpty()
                            val showArtists =
                                (
                                    uiState.selectedFilter == FavoritesFilter.ALL ||
                                        uiState.selectedFilter == FavoritesFilter.ARTISTS
                                    ) &&
                                    uiState.filteredArtists.isNotEmpty()

                            if (showSongs) {
                                if (uiState.selectedFilter == FavoritesFilter.ALL) {
                                    stickyHeader(key = "header_songs") {
                                        SectionHeader(title = stringResource(R.string.favorites_filter_songs))
                                    }
                                }

                                itemsIndexed(
                                    items = uiState.filteredSongs,
                                    key = { index, song -> "song_${index}_${song.id}" }
                                ) { index, song ->
                                    val artistName = song.artist.sanitizeForUi(stringResource(R.string.unknown_artist))
                                    val albumName = song.album.sanitizeForUi(stringResource(R.string.unknown_album))
                                    val subtitle = "$artistName • $albumName"

                                    com.vsulimov.resonance.ui.components.SongMediaListItem(
                                        song = song,
                                        title = song.title.sanitizeForUi(stringResource(R.string.unknown_title)),
                                        subtitle = subtitle,
                                        coverArtId = song.coverArt,
                                        placeholderIcon = Icons.Default.MusicNote,
                                        onClick = { onPlayQueue(uiState.filteredSongs, index) },
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

                            if (showAlbums) {
                                if (uiState.selectedFilter == FavoritesFilter.ALL) {
                                    stickyHeader(key = "header_albums") {
                                        SectionHeader(title = stringResource(R.string.favorites_filter_albums))
                                    }
                                }

                                items(
                                    items = uiState.filteredAlbums,
                                    key = { album -> "album_${album.id}" }
                                ) { album ->
                                    val albumSongIds = remember(album, downloadTasks) { album.songs.map { it.id } }
                                    val albumStatus = DownloadUtils.resolveCollectionDownloadStatus(
                                        songIds = albumSongIds,
                                        downloadTasks = downloadTasks,
                                        storageManager = appContainer.offlineStorageManager,
                                        expectedTotalCount = album.songCount ?: album.songs.size
                                    )

                                    MediaListItem(
                                        title = album.name.sanitizeForUi(stringResource(R.string.unknown_album)),
                                        subtitle = album.artist.sanitizeForUi(stringResource(R.string.unknown_artist)),
                                        coverArtId = album.coverArt,
                                        placeholderIcon = Icons.Default.Album,
                                        onClick = { onAlbumClick(album.id) },
                                        isDownloaded = albumStatus == DownloadStatus.DOWNLOADED,
                                        downloadStatus = albumStatus
                                    )
                                }
                            }

                            if (showArtists) {
                                if (uiState.selectedFilter == FavoritesFilter.ALL) {
                                    stickyHeader(key = "header_artists") {
                                        SectionHeader(title = stringResource(R.string.favorites_filter_artists))
                                    }
                                }

                                items(
                                    items = uiState.filteredArtists,
                                    key = { artist -> "artist_${artist.id}" }
                                ) { artist ->
                                    val subtitle = if (artist.albumCount > 0) {
                                        stringResource(R.string.album_count_format, artist.albumCount)
                                    } else {
                                        null
                                    }

                                    MediaListItem(
                                        title = artist.name.sanitizeForUi(stringResource(R.string.unknown_artist)),
                                        subtitle = subtitle,
                                        coverArtId = artist.coverArt,
                                        placeholderIcon = Icons.Default.Person,
                                        isCircularArtwork = true,
                                        onClick = { onArtistClick(artist.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        ScrollToTopFab(
            visible = isScrollToTopVisible,
            onClick = {
                coroutineScope.launch {
                    listState.animateScrollToItem(0)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = Dimens.paddingMedium + rightInset,
                    bottom = Dimens.paddingMedium + innerPadding.calculateBottomPadding()
                )
        )
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
                pendingRemoveSong = song
            },
            onToggleFavorite = { onToggleSongFavorite(song) },
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

    pendingRemoveSong?.let { song ->
        RemoveDownloadDialog(
            itemName = song.title,
            onConfirm = {
                coroutineScope.launch { appContainer.downloadRepository.deleteDownloadedSong(song.id) }
                pendingRemoveSong = null
            },
            onDismiss = { pendingRemoveSong = null }
        )
    }
}

/**
 * Horizontal row of Material 3 filter chips for switching between starred content types.
 *
 * @param selectedFilter Currently active [FavoritesFilter].
 * @param contentPadding Padding values for horizontal scroll bounds.
 * @param onFilterSelect Callback triggered when a filter chip is tapped.
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
private fun FilterChipsRow(
    selectedFilter: FavoritesFilter,
    contentPadding: PaddingValues,
    onFilterSelect: (FavoritesFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(Dimens.paddingSmall)
    ) {
        item {
            FilterChip(
                selected = selectedFilter == FavoritesFilter.ALL,
                onClick = { onFilterSelect(FavoritesFilter.ALL) },
                label = { Text(text = stringResource(R.string.favorites_filter_all)) }
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == FavoritesFilter.SONGS,
                onClick = { onFilterSelect(FavoritesFilter.SONGS) },
                label = { Text(text = stringResource(R.string.favorites_filter_songs)) }
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == FavoritesFilter.ALBUMS,
                onClick = { onFilterSelect(FavoritesFilter.ALBUMS) },
                label = { Text(text = stringResource(R.string.favorites_filter_albums)) }
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == FavoritesFilter.ARTISTS,
                onClick = { onFilterSelect(FavoritesFilter.ARTISTS) },
                label = { Text(text = stringResource(R.string.favorites_filter_artists)) }
            )
        }
    }
}

/**
 * Sticky section header label for categorized favorite item groups.
 *
 * @param title Section header title text.
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(
                horizontal = Dimens.paddingMedium,
                vertical = Dimens.paddingSmall
            )
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
