package com.vsulimov.resonance.ui.screens.search

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
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
import com.vsulimov.resonance.ui.components.EmptyState
import com.vsulimov.resonance.ui.components.ErrorState
import com.vsulimov.resonance.ui.components.LoadingState
import com.vsulimov.resonance.ui.components.MediaListItem
import com.vsulimov.resonance.ui.components.RemoveDownloadDialog
import com.vsulimov.resonance.ui.components.ScrollToTopFab
import com.vsulimov.resonance.ui.components.SongOptionsBottomSheet
import com.vsulimov.resonance.ui.theme.Dimens
import kotlinx.coroutines.launch

/** Number of list items scrolled before displaying the scroll-to-top floating action button. */
private const val SCROLL_TO_TOP_THRESHOLD = 3

/**
 * Stateful entry composable for the Search Screen.
 *
 * @param innerPadding Padding values provided by the root Scaffold.
 * @param modifier The modifier to be applied to the layout.
 * @param onAlbumClick Callback triggered when an album item is tapped.
 * @param onArtistClick Callback triggered when an artist item is tapped.
 * @param viewModel ViewModel holding and managing the screen state.
 */
@Composable
fun SearchScreen(
    innerPadding: PaddingValues,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = viewModel(factory = SearchViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val appContainer = getAppContainer()
    val playerController = appContainer.playerController
    val favoritesRepository = appContainer.favoritesRepository

    val starredSongIds by favoritesRepository.starredSongIds.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    SearchContent(
        uiState = uiState,
        innerPadding = innerPadding,
        onSearchQueryChange = viewModel::onSearchQueryChanged,
        onClearQuery = viewModel::clearQuery,
        onFilterSelect = viewModel::selectFilter,
        onRetryClick = viewModel::retry,
        onPlayQueue = { songs, index -> playerController.playQueue(songs, index) },
        onPlayNext = playerController::playNext,
        onAddToQueue = playerController::addToQueue,
        starredSongIds = starredSongIds,
        onToggleSongFavorite = { song ->
            coroutineScope.launch {
                favoritesRepository.toggleSongFavorite(song.id)
            }
        },
        onAlbumClick = onAlbumClick,
        onArtistClick = onArtistClick,
        modifier = modifier
    )
}

/**
 * Stateless content composable for search input, filter chips, and sectioned search results.
 *
 * @param uiState Current UI state containing query, selected filter, and results.
 * @param innerPadding Padding values provided by the root Scaffold.
 * @param onSearchQueryChange Callback triggered when user types in search bar.
 * @param onClearQuery Callback triggered when clear query icon button is tapped.
 * @param onFilterSelect Callback triggered when a search filter chip is selected.
 * @param onRetryClick Callback triggered on retrying failed search request.
 * @param onPlayQueue Callback triggered to play songs queue starting at index.
 * @param onPlayNext Callback triggered to queue song next.
 * @param onAddToQueue Callback triggered to append song to queue.
 * @param onAlbumClick Callback triggered when an album result item is clicked.
 * @param onArtistClick Callback triggered when an artist result item is clicked.
 * @param modifier The modifier to be applied to the layout.
 * @param starredSongIds Set of starred song IDs for resolving favorite status.
 * @param onToggleSongFavorite Callback triggered when user toggles favorite status for a song.
 */
@Composable
fun SearchContent(
    uiState: SearchUiState,
    innerPadding: PaddingValues,
    onSearchQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onFilterSelect: (SearchFilter) -> Unit,
    onRetryClick: () -> Unit,
    onPlayQueue: (List<Child>, Int) -> Unit,
    onPlayNext: (Child) -> Unit,
    onAddToQueue: (Child) -> Unit,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onToggleSongFavorite: (Child) -> Unit,
    modifier: Modifier = Modifier,
    starredSongIds: Set<String> = emptySet()
) {
    val safeInsets = WindowInsets.safeDrawing.asPaddingValues()
    val leftInset = safeInsets.calculateLeftPadding(LayoutDirection.Ltr)
    val rightInset = safeInsets.calculateRightPadding(LayoutDirection.Ltr)

    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var selectedSongForOptions by remember { mutableStateOf<Child?>(null) }
    var pendingRemoveSong by remember { mutableStateOf<Child?>(null) }

    val appContainer = getAppContainer()
    val downloadTasks by appContainer.downloadRepository.observeAllTasksSimplified().collectAsStateWithLifecycle(
        initialValue = emptyList()
    )

    val isScrollToTopVisible by remember {
        derivedStateOf { listState.firstVisibleItemIndex > SCROLL_TO_TOP_THRESHOLD }
    }

    val totalResultsCount = uiState.songs.size + uiState.albums.size + uiState.artists.size

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
            SearchBarInput(
                query = uiState.searchQuery,
                onQueryChange = onSearchQueryChange,
                onClearClick = onClearQuery,
                onSearchAction = { focusManager.clearFocus() },
                modifier = Modifier.padding(
                    start = Dimens.paddingLarge + leftInset,
                    end = Dimens.paddingLarge + rightInset
                )
            )

            Spacer(modifier = Modifier.height(Dimens.paddingSmall))

            SearchFilterChipsRow(
                selectedFilter = uiState.selectedFilter,
                contentPadding = PaddingValues(
                    start = Dimens.paddingLarge + leftInset,
                    end = Dimens.paddingLarge + rightInset
                ),
                onFilterSelect = onFilterSelect
            )

            Spacer(modifier = Modifier.height(Dimens.paddingSmall))

            when {
                uiState.searchQuery.isBlank() -> {
                    EmptyState(
                        icon = Icons.Default.Search,
                        title = stringResource(R.string.search_empty_prompt_title),
                        description = stringResource(R.string.search_empty_prompt_desc),
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

                totalResultsCount == 0 -> {
                    EmptyState(
                        icon = Icons.Default.SearchOff,
                        title = stringResource(R.string.search_no_results_title),
                        description = stringResource(R.string.search_no_results_desc, uiState.searchQuery),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding())
                    )
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            bottom = Dimens.paddingLarge + innerPadding.calculateBottomPadding(),
                            start = leftInset,
                            end = rightInset
                        )
                    ) {
                        val showSongs =
                            (
                                (uiState.selectedFilter == SearchFilter.ALL) ||
                                    (uiState.selectedFilter == SearchFilter.SONGS)
                                ) &&
                                uiState.songs.isNotEmpty()
                        val showAlbums =
                            (
                                (uiState.selectedFilter == SearchFilter.ALL) ||
                                    (uiState.selectedFilter == SearchFilter.ALBUMS)
                                ) &&
                                uiState.albums.isNotEmpty()
                        val showArtists =
                            (
                                (uiState.selectedFilter == SearchFilter.ALL) ||
                                    (uiState.selectedFilter == SearchFilter.ARTISTS)
                                ) &&
                                uiState.artists.isNotEmpty()

                        if (showSongs) {
                            stickyHeader(key = "header_songs") {
                                SearchSectionHeader(title = stringResource(R.string.search_section_songs))
                            }

                            itemsIndexed(
                                items = uiState.songs,
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
                                    onClick = { onPlayQueue(uiState.songs, index) },
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
                            stickyHeader(key = "header_albums") {
                                SearchSectionHeader(title = stringResource(R.string.search_section_albums))
                            }

                            items(
                                items = uiState.albums,
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
                            stickyHeader(key = "header_artists") {
                                SearchSectionHeader(title = stringResource(R.string.search_section_artists))
                            }

                            items(
                                items = uiState.artists,
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
 * Custom search input bar with leading search icon and trailing clear button.
 *
 * @param query Current text query in the search bar.
 * @param onQueryChange Callback triggered on user text input.
 * @param onClearClick Callback triggered when clear query button is clicked.
 * @param onSearchAction Callback triggered when search keyboard action is executed.
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
private fun SearchBarInput(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearClick: () -> Unit,
    onSearchAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                text = stringResource(R.string.search_input_placeholder),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClearClick) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.cd_clear_search),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Search
        ),
        keyboardActions = KeyboardActions(
            onSearch = { onSearchAction() }
        ),
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    )
}

/**
 * Horizontal row of filter chips for restricting search results by category.
 *
 * @param selectedFilter Currently active [SearchFilter].
 * @param contentPadding Horizontal padding values for row bounds.
 * @param onFilterSelect Callback triggered when a filter chip is tapped.
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
private fun SearchFilterChipsRow(
    selectedFilter: SearchFilter,
    contentPadding: PaddingValues,
    onFilterSelect: (SearchFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(Dimens.paddingSmall)
    ) {
        item {
            FilterChip(
                selected = selectedFilter == SearchFilter.ALL,
                onClick = { onFilterSelect(SearchFilter.ALL) },
                label = { Text(text = stringResource(R.string.search_filter_all)) }
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == SearchFilter.SONGS,
                onClick = { onFilterSelect(SearchFilter.SONGS) },
                label = { Text(text = stringResource(R.string.search_filter_songs)) }
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == SearchFilter.ALBUMS,
                onClick = { onFilterSelect(SearchFilter.ALBUMS) },
                label = { Text(text = stringResource(R.string.search_filter_albums)) }
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == SearchFilter.ARTISTS,
                onClick = { onFilterSelect(SearchFilter.ARTISTS) },
                label = { Text(text = stringResource(R.string.search_filter_artists)) }
            )
        }
    }
}

/**
 * Sticky section header label for categorized search result groups.
 *
 * @param title Section header title text.
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
private fun SearchSectionHeader(title: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(
                horizontal = Dimens.paddingLarge,
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
