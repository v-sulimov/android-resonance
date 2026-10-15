package com.vsulimov.resonance.ui.screens.downloads

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsulimov.resonance.R
import com.vsulimov.resonance.core.Constants
import com.vsulimov.resonance.core.ui.CoverImage
import com.vsulimov.resonance.core.utils.FileUtils
import com.vsulimov.resonance.core.utils.sanitizeForUi
import com.vsulimov.resonance.data.db.entity.OfflineSongEntity
import com.vsulimov.resonance.ui.theme.Dimens

/** Height of the M3 storage usage linear progress indicator. */
private val STORAGE_PROGRESS_HEIGHT = 8.dp

/** Size of the icon container in empty category placeholders. */
private val EMPTY_STATE_ICON_CONTAINER_SIZE = 64.dp

/**
 * Stateful entry composable for the Manage Downloads Sub-Screen.
 *
 * @param innerPadding Padding values provided by outer scaffold.
 * @param onBackClick Callback triggered when back navigation is tapped.
 * @param modifier The modifier to be applied to the layout.
 * @param viewModel ViewModel holding screen state.
 */
@Composable
fun ManageDownloadsScreen(
    innerPadding: PaddingValues,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DownloadsViewModel = viewModel(factory = DownloadsViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ManageDownloadsContent(
        uiState = uiState,
        innerPadding = innerPadding,
        onBackClick = onBackClick,
        onTabSelected = viewModel::onTabSelected,
        onSortCriteriaSelected = viewModel::onSortCriteriaSelected,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onToggleArtistExpanded = viewModel::toggleArtistExpanded,
        onToggleAlbumExpanded = viewModel::toggleAlbumExpanded,
        onSetPendingDeleteTarget = viewModel::setPendingDeleteTarget,
        onRemoveAllClick = { viewModel.setRemoveAllDialogOpen(true) },
        modifier = modifier
    )

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

    uiState.pendingDeleteTarget?.let { target ->
        DeleteTargetConfirmationDialog(
            target = target,
            onConfirm = viewModel::confirmPendingDelete,
            onDismiss = { viewModel.setPendingDeleteTarget(null) }
        )
    }
}

/**
 * Stateless content composable displaying Hero Storage card, search bar, category chips, and deletion hierarchy.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageDownloadsContent(
    uiState: DownloadsUiState,
    innerPadding: PaddingValues,
    onBackClick: () -> Unit,
    onTabSelected: (DownloadedTab) -> Unit,
    onSortCriteriaSelected: (DownloadSortCriteria) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onToggleArtistExpanded: (String) -> Unit,
    onToggleAlbumExpanded: (String) -> Unit,
    onSetPendingDeleteTarget: (DeleteTarget?) -> Unit,
    onRemoveAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val safeInsets = WindowInsets.safeDrawing.asPaddingValues()
    val leftInset = safeInsets.calculateLeftPadding(LayoutDirection.Ltr)
    val rightInset = safeInsets.calculateRightPadding(LayoutDirection.Ltr)
    var isMenuOpen by remember { mutableStateOf(false) }
    var isSortMenuOpen by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.manage_downloads_screen_title),
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
                    if (uiState.downloadedSongs.isNotEmpty()) {
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
                start = leftInset,
                top = scaffoldPadding.calculateTopPadding() + Dimens.paddingSmall,
                end = rightInset,
                bottom = innerPadding.calculateBottomPadding() + Dimens.paddingLarge
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.paddingMedium)
        ) {
            item(key = "storage_hero_card") {
                val totalUsedText = FileUtils.formatFileSize(uiState.totalOfflineStorageBytes)
                val freeText = FileUtils.formatFileSize(uiState.freeDiskBytes)

                val storageRatio = if (uiState.totalDiskBytes > 0) {
                    (uiState.totalOfflineStorageBytes.toFloat() / uiState.totalDiskBytes).coerceIn(0.01f, 1f)
                } else {
                    0f
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.paddingMedium),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimens.paddingLarge)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.downloads_hero_storage_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(Dimens.paddingSmall))

                        Text(
                            text = totalUsedText,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(Dimens.paddingExtraSmall))

                        Text(
                            text = stringResource(
                                R.string.downloads_hero_storage_stat_format,
                                totalUsedText,
                                freeText
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(Dimens.paddingMedium))

                        LinearProgressIndicator(
                            progress = { storageRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(STORAGE_PROGRESS_HEIGHT)
                                .clip(MaterialTheme.shapes.small),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(Dimens.paddingMedium))

                        Text(
                            text = stringResource(
                                R.string.downloads_hero_content_summary_format,
                                uiState.downloadedSongs.size,
                                uiState.downloadedAlbums.size,
                                uiState.downloadedArtists.size
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            val hasDownloadedContent = uiState.downloadedSongs.isNotEmpty() ||
                uiState.downloadedAlbums.isNotEmpty() ||
                uiState.downloadedArtists.isNotEmpty() ||
                uiState.downloadedPlaylists.isNotEmpty()

            if (hasDownloadedContent) {
                item(key = "downloaded_search_bar") {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = onSearchQueryChanged,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dimens.paddingMedium),
                        placeholder = { Text(stringResource(R.string.downloads_search_placeholder)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null
                            )
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChanged("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = stringResource(R.string.cd_clear_text)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = MaterialTheme.shapes.large
                    )
                }

                item(key = "downloaded_filter_and_sort") {
                    val sortLabelRes = when (uiState.selectedSortCriteria) {
                        DownloadSortCriteria.SIZE_DESC -> R.string.sort_size_desc
                        DownloadSortCriteria.NAME_ASC -> R.string.sort_name_asc
                        DownloadSortCriteria.SONG_COUNT -> R.string.sort_song_count
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.paddingSmall),
                        contentPadding = PaddingValues(
                            start = Dimens.paddingMedium,
                            end = Dimens.paddingMedium
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            Box {
                                FilterChip(
                                    selected = true,
                                    onClick = { isSortMenuOpen = true },
                                    label = { Text(stringResource(sortLabelRes)) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Sort,
                                            contentDescription = null,
                                            modifier = Modifier.size(Dimens.iconSmall)
                                        )
                                    },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            modifier = Modifier.size(Dimens.iconSmall)
                                        )
                                    }
                                )
                                DropdownMenu(
                                    expanded = isSortMenuOpen,
                                    onDismissRequest = { isSortMenuOpen = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.sort_size_desc)) },
                                        onClick = {
                                            isSortMenuOpen = false
                                            onSortCriteriaSelected(DownloadSortCriteria.SIZE_DESC)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.sort_name_asc)) },
                                        onClick = {
                                            isSortMenuOpen = false
                                            onSortCriteriaSelected(DownloadSortCriteria.NAME_ASC)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.sort_song_count)) },
                                        onClick = {
                                            isSortMenuOpen = false
                                            onSortCriteriaSelected(DownloadSortCriteria.SONG_COUNT)
                                        }
                                    )
                                }
                            }
                        }

                        item {
                            FilterChip(
                                selected = uiState.selectedTab == DownloadedTab.ARTISTS,
                                onClick = { onTabSelected(DownloadedTab.ARTISTS) },
                                label = {
                                    Text(stringResource(R.string.downloads_tab_artists, uiState.downloadedArtists.size))
                                }
                            )
                        }
                        item {
                            FilterChip(
                                selected = uiState.selectedTab == DownloadedTab.ALBUMS,
                                onClick = { onTabSelected(DownloadedTab.ALBUMS) },
                                label = {
                                    Text(stringResource(R.string.downloads_tab_albums, uiState.downloadedAlbums.size))
                                }
                            )
                        }
                        item {
                            FilterChip(
                                selected = uiState.selectedTab == DownloadedTab.SONGS,
                                onClick = { onTabSelected(DownloadedTab.SONGS) },
                                label = {
                                    Text(stringResource(R.string.downloads_tab_songs, uiState.downloadedSongs.size))
                                }
                            )
                        }
                        item {
                            FilterChip(
                                selected = uiState.selectedTab == DownloadedTab.PLAYLISTS,
                                onClick = { onTabSelected(DownloadedTab.PLAYLISTS) },
                                label = {
                                    Text(
                                        stringResource(
                                            R.string.downloads_tab_playlists,
                                            uiState.downloadedPlaylists.size
                                        )
                                    )
                                }
                            )
                        }
                    }
                }

                val query = uiState.searchQuery.trim().lowercase()

                when (uiState.selectedTab) {
                    DownloadedTab.ARTISTS -> {
                        val filteredArtists = uiState.downloadedArtists
                            .filter { artistItem ->
                                query.isEmpty() || artistItem.artist.name.lowercase().contains(query)
                            }
                            .let { list ->
                                when (uiState.selectedSortCriteria) {
                                    DownloadSortCriteria.SIZE_DESC -> list.sortedByDescending { it.totalBytes }
                                    DownloadSortCriteria.NAME_ASC -> list.sortedBy { it.artist.name }
                                    DownloadSortCriteria.SONG_COUNT -> list.sortedByDescending { it.songCount }
                                }
                            }

                        if (filteredArtists.isEmpty()) {
                            item(key = "empty_artists_tab") {
                                if (query.isNotEmpty()) {
                                    EmptySearchPlaceholder(
                                        modifier = Modifier.padding(horizontal = Dimens.paddingMedium)
                                    )
                                } else {
                                    EmptyCategoryPlaceholder(
                                        icon = Icons.Default.Person,
                                        title = stringResource(R.string.downloads_empty_category_artists_title),
                                        description = stringResource(R.string.downloads_empty_category_artists_desc),
                                        modifier = Modifier.padding(horizontal = Dimens.paddingMedium)
                                    )
                                }
                            }
                        } else {
                            filteredArtists.forEach { artistItem ->
                                val isArtistExpanded = uiState.expandedArtistIds.contains(artistItem.artist.id)

                                item(key = "artist_${artistItem.artist.id}") {
                                    DownloadedArtistRow(
                                        artistItem = artistItem,
                                        isExpanded = isArtistExpanded,
                                        onToggleExpand = { onToggleArtistExpanded(artistItem.artist.id) },
                                        onDeleteClick = {
                                            onSetPendingDeleteTarget(DeleteTarget.Artist(artistItem))
                                        },
                                        modifier = Modifier.padding(horizontal = Dimens.paddingMedium)
                                    )
                                }

                                if (isArtistExpanded) {
                                    val artistAlbums = uiState.downloadedAlbums
                                        .filter {
                                            it.album.artistId == artistItem.artist.id ||
                                                (
                                                    it.album.artist != null &&
                                                        it.album.artist.equals(
                                                            artistItem.artist.name,
                                                            ignoreCase = true
                                                        )
                                                    )
                                        }
                                        .sortedByDescending { it.totalBytes }

                                    val artistSongs = uiState.downloadedSongs
                                        .filter {
                                            it.artistId == artistItem.artist.id ||
                                                (
                                                    it.artist != null &&
                                                        it.artist.equals(artistItem.artist.name, ignoreCase = true)
                                                    )
                                        }
                                        .sortedByDescending { it.size ?: 0L }

                                    if (artistAlbums.isNotEmpty()) {
                                        artistAlbums.forEach { albumItem ->
                                            val isAlbumExpanded = uiState.expandedAlbumIds.contains(albumItem.album.id)

                                            item(key = "nested_album_${albumItem.album.id}") {
                                                DownloadedAlbumRow(
                                                    albumItem = albumItem,
                                                    isExpanded = isAlbumExpanded,
                                                    isNested = true,
                                                    onToggleExpand = { onToggleAlbumExpanded(albumItem.album.id) },
                                                    onDeleteClick = {
                                                        onSetPendingDeleteTarget(DeleteTarget.Album(albumItem))
                                                    },
                                                    modifier = Modifier.padding(horizontal = Dimens.paddingMedium)
                                                )
                                            }

                                            if (isAlbumExpanded) {
                                                val albumSongs = artistSongs.filter {
                                                    it.albumId == albumItem.album.id ||
                                                        (
                                                            it.album != null &&
                                                                it.album.equals(albumItem.album.name, ignoreCase = true)
                                                            )
                                                }

                                                items(
                                                    items = albumSongs,
                                                    key = { song -> "nested_song_${song.id}" }
                                                ) { song ->
                                                    DownloadedSongRow(
                                                        song = song,
                                                        isNested = true,
                                                        onDeleteClick = {
                                                            onSetPendingDeleteTarget(DeleteTarget.Song(song))
                                                        },
                                                        modifier = Modifier.padding(horizontal = Dimens.paddingMedium)
                                                    )
                                                }
                                            }
                                        }
                                    } else if (artistSongs.isNotEmpty()) {
                                        items(
                                            items = artistSongs,
                                            key = { song -> "nested_loose_song_${song.id}" }
                                        ) { song ->
                                            DownloadedSongRow(
                                                song = song,
                                                isNested = true,
                                                onDeleteClick = {
                                                    onSetPendingDeleteTarget(DeleteTarget.Song(song))
                                                },
                                                modifier = Modifier.padding(horizontal = Dimens.paddingMedium)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    DownloadedTab.ALBUMS -> {
                        val filteredAlbums = uiState.downloadedAlbums
                            .filter { albumItem ->
                                query.isEmpty() ||
                                    albumItem.album.name.lowercase().contains(query) ||
                                    (albumItem.album.artist?.lowercase()?.contains(query) == true)
                            }
                            .let { list ->
                                when (uiState.selectedSortCriteria) {
                                    DownloadSortCriteria.SIZE_DESC -> list.sortedByDescending { it.totalBytes }
                                    DownloadSortCriteria.NAME_ASC -> list.sortedBy { it.album.name }
                                    DownloadSortCriteria.SONG_COUNT -> list.sortedByDescending { it.songCount }
                                }
                            }

                        if (filteredAlbums.isEmpty()) {
                            item(key = "empty_albums_tab") {
                                if (query.isNotEmpty()) {
                                    EmptySearchPlaceholder(
                                        modifier = Modifier.padding(horizontal = Dimens.paddingMedium)
                                    )
                                } else {
                                    EmptyCategoryPlaceholder(
                                        icon = Icons.Default.Album,
                                        title = stringResource(R.string.downloads_empty_category_albums_title),
                                        description = stringResource(R.string.downloads_empty_category_albums_desc),
                                        modifier = Modifier.padding(horizontal = Dimens.paddingMedium)
                                    )
                                }
                            }
                        } else {
                            filteredAlbums.forEach { albumItem ->
                                val isAlbumExpanded = uiState.expandedAlbumIds.contains(albumItem.album.id)

                                item(key = "album_${albumItem.album.id}") {
                                    DownloadedAlbumRow(
                                        albumItem = albumItem,
                                        isExpanded = isAlbumExpanded,
                                        isNested = false,
                                        onToggleExpand = { onToggleAlbumExpanded(albumItem.album.id) },
                                        onDeleteClick = {
                                            onSetPendingDeleteTarget(DeleteTarget.Album(albumItem))
                                        },
                                        modifier = Modifier.padding(horizontal = Dimens.paddingMedium)
                                    )
                                }

                                if (isAlbumExpanded) {
                                    val albumSongs = uiState.downloadedSongs
                                        .filter {
                                            it.albumId == albumItem.album.id ||
                                                (
                                                    it.album != null &&
                                                        it.album.equals(albumItem.album.name, ignoreCase = true)
                                                    )
                                        }
                                        .sortedByDescending { it.size ?: 0L }

                                    items(
                                        items = albumSongs,
                                        key = { song -> "nested_album_song_${song.id}" }
                                    ) { song ->
                                        DownloadedSongRow(
                                            song = song,
                                            isNested = true,
                                            onDeleteClick = {
                                                onSetPendingDeleteTarget(DeleteTarget.Song(song))
                                            },
                                            modifier = Modifier.padding(horizontal = Dimens.paddingMedium)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    DownloadedTab.SONGS -> {
                        val filteredSongs = uiState.downloadedSongs
                            .filter { song ->
                                query.isEmpty() ||
                                    song.title.lowercase().contains(query) ||
                                    (song.artist?.lowercase()?.contains(query) == true) ||
                                    (song.album?.lowercase()?.contains(query) == true)
                            }
                            .let { list ->
                                when (uiState.selectedSortCriteria) {
                                    DownloadSortCriteria.SIZE_DESC -> list.sortedByDescending { it.size ?: 0L }
                                    DownloadSortCriteria.NAME_ASC -> list.sortedBy { it.title }
                                    DownloadSortCriteria.SONG_COUNT -> list.sortedByDescending { it.size ?: 0L }
                                }
                            }

                        if (filteredSongs.isEmpty()) {
                            item(key = "empty_songs_tab") {
                                if (query.isNotEmpty()) {
                                    EmptySearchPlaceholder(
                                        modifier = Modifier.padding(horizontal = Dimens.paddingMedium)
                                    )
                                } else {
                                    EmptyCategoryPlaceholder(
                                        icon = Icons.Default.MusicNote,
                                        title = stringResource(R.string.downloads_empty_category_songs_title),
                                        description = stringResource(R.string.downloads_empty_category_songs_desc),
                                        modifier = Modifier.padding(horizontal = Dimens.paddingMedium)
                                    )
                                }
                            }
                        } else {
                            items(
                                items = filteredSongs,
                                key = { song -> "song_${song.id}" }
                            ) { song ->
                                DownloadedSongRow(
                                    song = song,
                                    isNested = false,
                                    onDeleteClick = { onSetPendingDeleteTarget(DeleteTarget.Song(song)) },
                                    modifier = Modifier.padding(horizontal = Dimens.paddingMedium)
                                )
                            }
                        }
                    }

                    DownloadedTab.PLAYLISTS -> {
                        val filteredPlaylists = uiState.downloadedPlaylists
                            .filter { playlistItem ->
                                query.isEmpty() || playlistItem.playlist.name.lowercase().contains(query)
                            }
                            .let { list ->
                                when (uiState.selectedSortCriteria) {
                                    DownloadSortCriteria.SIZE_DESC -> list.sortedByDescending { it.totalBytes }
                                    DownloadSortCriteria.NAME_ASC -> list.sortedBy { it.playlist.name }
                                    DownloadSortCriteria.SONG_COUNT -> list.sortedByDescending { it.songCount }
                                }
                            }

                        if (filteredPlaylists.isEmpty()) {
                            item(key = "empty_playlists_tab") {
                                if (query.isNotEmpty()) {
                                    EmptySearchPlaceholder(
                                        modifier = Modifier.padding(horizontal = Dimens.paddingMedium)
                                    )
                                } else {
                                    EmptyCategoryPlaceholder(
                                        icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                                        title = stringResource(R.string.downloads_empty_category_playlists_title),
                                        description = stringResource(R.string.downloads_empty_category_playlists_desc),
                                        modifier = Modifier.padding(horizontal = Dimens.paddingMedium)
                                    )
                                }
                            }
                        } else {
                            items(
                                items = filteredPlaylists,
                                key = { playlistItem -> "playlist_${playlistItem.playlist.id}" }
                            ) { playlistItem ->
                                DownloadedPlaylistRow(
                                    playlistItem = playlistItem,
                                    onDeleteClick = {
                                        onSetPendingDeleteTarget(DeleteTarget.Playlist(playlistItem))
                                    },
                                    modifier = Modifier.padding(horizontal = Dimens.paddingMedium)
                                )
                            }
                        }
                    }
                }
            } else {
                item(key = "empty_downloaded_state") {
                    EmptyCategoryPlaceholder(
                        icon = Icons.Default.Storage,
                        title = stringResource(R.string.downloads_empty_downloaded),
                        description = stringResource(R.string.downloads_empty_category_songs_desc),
                        modifier = Modifier.padding(horizontal = Dimens.paddingMedium)
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadedArtistRow(
    artistItem: DownloadedArtistItem,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val artist = artistItem.artist
    val nameText = artist.name.sanitizeForUi(stringResource(R.string.unknown_artist))
    val sizeText = FileUtils.formatFileSize(artistItem.totalBytes)
    val subtitleText = stringResource(
        R.string.downloads_artist_subtitle_format,
        artistItem.albumCount,
        artistItem.songCount,
        sizeText
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleExpand)
            .padding(vertical = Dimens.paddingSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(Dimens.iconExtraLarge)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(Dimens.iconMedium)
            )
        }

        Spacer(modifier = Modifier.width(Dimens.paddingMedium))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = nameText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = if (isExpanded) {
                stringResource(
                    R.string.cd_collapse_item
                )
            } else {
                stringResource(R.string.cd_expand_item)
            },
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        IconButton(onClick = onDeleteClick) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.cd_delete_download),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun DownloadedAlbumRow(
    albumItem: DownloadedAlbumItem,
    isExpanded: Boolean,
    isNested: Boolean,
    onToggleExpand: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val album = albumItem.album
    val nameText = album.name.sanitizeForUi(stringResource(R.string.home_unknown_album))
    val artistText = album.artist?.sanitizeForUi(stringResource(R.string.unknown_artist))
    val sizeText = FileUtils.formatFileSize(albumItem.totalBytes)
    val subtitleText = stringResource(R.string.downloads_album_subtitle_format, albumItem.songCount, sizeText)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = if (isNested) Dimens.paddingLarge else 0.dp)
            .clickable(onClick = onToggleExpand)
            .padding(vertical = Dimens.paddingSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(Dimens.iconExtraLarge)
                .clip(MaterialTheme.shapes.small),
            contentAlignment = Alignment.Center
        ) {
            if (album.coverArt != null) {
                CoverImage(
                    coverArtId = album.coverArt,
                    size = Constants.COVER_ART_THUMBNAIL_SIZE,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Album,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(Dimens.paddingMedium))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = nameText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (artistText != null) "$artistText • $subtitleText" else subtitleText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = if (isExpanded) {
                stringResource(
                    R.string.cd_collapse_item
                )
            } else {
                stringResource(R.string.cd_expand_item)
            },
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        IconButton(onClick = onDeleteClick) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.cd_delete_download),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun DownloadedSongRow(
    song: OfflineSongEntity,
    isNested: Boolean,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val titleText = song.title.sanitizeForUi(stringResource(R.string.unknown_title))
    val artistText = song.artist?.sanitizeForUi(stringResource(R.string.unknown_artist))
    val sizeText = song.size?.let { FileUtils.formatFileSize(it) }
    val subtitleText = if (sizeText != null && artistText != null) {
        "$artistText • $sizeText"
    } else {
        artistText ?: sizeText ?: ""
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = if (isNested) Dimens.paddingExtraLarge else 0.dp)
            .clickable(onClick = onDeleteClick)
            .padding(vertical = Dimens.paddingSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(Dimens.iconExtraLarge)
                .clip(MaterialTheme.shapes.small),
            contentAlignment = Alignment.Center
        ) {
            if (song.coverArt != null) {
                CoverImage(
                    coverArtId = song.coverArt,
                    size = Constants.COVER_ART_THUMBNAIL_SIZE,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(Dimens.paddingMedium))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titleText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onDeleteClick) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.cd_delete_download),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun DownloadedPlaylistRow(
    playlistItem: DownloadedPlaylistItem,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playlist = playlistItem.playlist
    val nameText = playlist.name.sanitizeForUi(stringResource(R.string.unknown_title))
    val sizeText = FileUtils.formatFileSize(playlistItem.totalBytes)
    val subtitleText = stringResource(R.string.downloads_playlist_subtitle_format, playlistItem.songCount, sizeText)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onDeleteClick)
            .padding(vertical = Dimens.paddingSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(Dimens.iconExtraLarge)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.tertiaryContainer),
            contentAlignment = Alignment.Center
        ) {
            if (playlist.coverArt != null) {
                CoverImage(
                    coverArtId = playlist.coverArt,
                    size = Constants.COVER_ART_THUMBNAIL_SIZE,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(Dimens.iconMedium)
                )
            }
        }

        Spacer(modifier = Modifier.width(Dimens.paddingMedium))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = nameText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onDeleteClick) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.cd_delete_download),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun EmptyCategoryPlaceholder(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.paddingExtraLarge, horizontal = Dimens.paddingLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(EMPTY_STATE_ICON_CONTAINER_SIZE)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Dimens.iconLarge)
            )
        }

        Spacer(modifier = Modifier.height(Dimens.paddingMedium))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(Dimens.paddingSmall))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EmptySearchPlaceholder(modifier: Modifier = Modifier) {
    EmptyCategoryPlaceholder(
        icon = Icons.Default.SearchOff,
        title = stringResource(R.string.downloads_empty_search_title),
        description = stringResource(R.string.downloads_empty_search_desc),
        modifier = modifier
    )
}

@Composable
private fun DeleteTargetConfirmationDialog(target: DeleteTarget, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val title: String
    val message: String

    when (target) {
        is DeleteTarget.Song -> {
            title = stringResource(R.string.dialog_remove_download_title)
            message = stringResource(R.string.dialog_remove_download_message, target.song.title)
        }

        is DeleteTarget.Album -> {
            title = stringResource(R.string.dialog_delete_album_title)
            message = stringResource(
                R.string.dialog_delete_album_message,
                target.item.album.name,
                FileUtils.formatFileSize(target.item.totalBytes),
                target.item.songCount
            )
        }

        is DeleteTarget.Artist -> {
            title = stringResource(R.string.dialog_delete_artist_title)
            message = stringResource(
                R.string.dialog_delete_artist_message,
                target.item.artist.name,
                FileUtils.formatFileSize(target.item.totalBytes),
                target.item.songCount
            )
        }

        is DeleteTarget.Playlist -> {
            title = stringResource(R.string.dialog_delete_playlist_title)
            message = stringResource(
                R.string.dialog_delete_playlist_message,
                target.item.playlist.name,
                FileUtils.formatFileSize(target.item.totalBytes),
                target.item.songCount
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(stringResource(R.string.dialog_remove_download_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_remove_download_cancel))
            }
        }
    )
}
