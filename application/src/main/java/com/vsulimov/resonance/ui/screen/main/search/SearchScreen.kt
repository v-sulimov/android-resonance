package com.vsulimov.resonance.ui.screen.main.search

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsulimov.resonance.R
import com.vsulimov.resonance.ui.component.CoverArtPlaceholder

private val SearchBarHorizontalPadding = 16.dp
private val SearchBarTopPadding = 8.dp
private val SearchBarBottomPadding = 12.dp
private val SearchBarCornerRadius = 28.dp
private val ChipsHorizontalPadding = 16.dp
private val ChipsBottomPadding = 12.dp
private val ChipSpacing = 8.dp
private val ThumbnailSize = 48.dp
private val ThumbnailCornerRadius = 8.dp
private val SectionTitlePadding = 16.dp
private val SectionBottomSpacing = 16.dp
private val HistoryIconSize = 20.dp

/** Separator between metadata parts (e.g. "Artist · Album"). */
private const val METADATA_SEPARATOR = " \u00B7 "

/**
 * Search screen displaying a search bar, filter chips, search history,
 * and grouped search results.
 *
 * Two primary visual states:
 * - **History**: When the query is empty, shows recent search entries.
 * - **Results**: When a query is active, shows filter chips and grouped
 *   results for artists, albums, and songs.
 *
 * @param innerPadding Padding from the parent Scaffold (includes bottom nav).
 * @param onAlbumClick Callback invoked when an album result or song is tapped.
 * @param modifier Modifier applied to the root layout.
 */
@Composable
fun SearchScreen(
    innerPadding: PaddingValues,
    onAlbumClick: (albumId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: SearchViewModel = viewModel(factory = SearchViewModel.Factory)
    val stateHolder = viewModel.stateHolder
    val uiState by stateHolder.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        SearchInputBar(
            query = uiState.query,
            onQueryChange = stateHolder::onQueryChange,
            onSearch = { stateHolder.onSearch(uiState.query) },
            onClear = stateHolder::onClearQuery
        )

        when (val resultState = uiState.resultState) {
            is SearchResultState.Idle -> SearchHistoryContent(
                history = uiState.history,
                onHistoryItemClick = { query ->
                    stateHolder.onSearch(query)
                },
                onRemoveHistoryItem = stateHolder::onRemoveHistoryEntry,
                onClearHistory = stateHolder::onClearHistory
            )
            is SearchResultState.Loading -> LoadingContent()
            is SearchResultState.Success -> {
                FilterChipsRow(
                    selectedFilter = uiState.filter,
                    onFilterChange = stateHolder::onFilterChange
                )
                SearchResultsContent(
                    artists = resultState.artists,
                    albums = resultState.albums,
                    songs = resultState.songs,
                    filter = uiState.filter,
                    query = uiState.query,
                    stateHolder = stateHolder,
                    onAlbumClick = onAlbumClick
                )
            }
            is SearchResultState.Empty -> EmptyResultsContent()
            is SearchResultState.Error -> ErrorResultsContent(message = resultState.message)
        }
    }
}

/**
 * Pill-shaped search input bar with leading search icon and trailing clear button.
 */
@Composable
private fun SearchInputBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SearchBarHorizontalPadding)
            .padding(top = SearchBarTopPadding, bottom = SearchBarBottomPadding),
        placeholder = {
            Text(text = stringResource(R.string.search_placeholder))
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null
            )
        },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.cd_clear_search)
                    )
                }
            }
        } else {
            null
        },
        shape = RoundedCornerShape(SearchBarCornerRadius),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() })
    )
}

/**
 * Search history list with "Recent searches" header and "Clear all" action.
 */
@Composable
private fun SearchHistoryContent(
    history: List<String>,
    onHistoryItemClick: (String) -> Unit,
    onRemoveHistoryItem: (String) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (history.isEmpty()) return

    LazyColumn(modifier = modifier.fillMaxSize()) {
        item(key = "history_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .padding(horizontal = SectionTitlePadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.search_recent_searches),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.search_clear_all),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onClearHistory)
                )
            }
            Spacer(modifier = Modifier.height(ChipSpacing))
        }

        items(
            items = history,
            key = { "history_$it" }
        ) { query ->
            ListItem(
                headlineContent = {
                    Text(
                        text = query,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        modifier = Modifier.size(HistoryIconSize),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingContent = {
                    IconButton(onClick = { onRemoveHistoryItem(query) }) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(
                                R.string.cd_remove_search_history,
                                query
                            ),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                modifier = Modifier.clickable { onHistoryItemClick(query) }
            )
        }
    }
}

/**
 * Horizontal row of filter chips for narrowing search results.
 */
@Composable
private fun FilterChipsRow(
    selectedFilter: SearchFilter,
    onFilterChange: (SearchFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ChipsHorizontalPadding)
            .padding(bottom = ChipsBottomPadding),
        horizontalArrangement = Arrangement.spacedBy(ChipSpacing)
    ) {
        SearchFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter == selectedFilter,
                onClick = { onFilterChange(filter) },
                label = { Text(text = stringResource(filter.labelResId)) }
            )
        }
    }
}

/**
 * Grouped search results list displaying artists, albums, and songs
 * filtered by the active [filter].
 */
@Composable
private fun SearchResultsContent(
    artists: List<SearchArtistViewObject>,
    albums: List<SearchAlbumViewObject>,
    songs: List<SearchSongViewObject>,
    filter: SearchFilter,
    query: String,
    stateHolder: SearchStateHolder,
    onAlbumClick: (albumId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val showArtists = (filter == SearchFilter.ALL || filter == SearchFilter.ARTISTS) && artists.isNotEmpty()
    val showAlbums = (filter == SearchFilter.ALL || filter == SearchFilter.ALBUMS) && albums.isNotEmpty()
    val showSongs = (filter == SearchFilter.ALL || filter == SearchFilter.SONGS) && songs.isNotEmpty()

    LazyColumn(modifier = modifier.fillMaxSize()) {
        if (showArtists) {
            item(key = "artists_header") {
                SectionHeader(title = stringResource(R.string.search_section_artists))
            }
            items(
                items = artists,
                key = { "artist_${it.id}" }
            ) { artist ->
                ArtistResultItem(
                    artist = artist,
                    query = query,
                    loadCoverArt = stateHolder::loadCoverArt,
                    getCachedCoverArt = stateHolder::getCachedCoverArt
                )
            }
            item(key = "artists_spacer") {
                Spacer(modifier = Modifier.height(SectionBottomSpacing))
            }
        }

        if (showAlbums) {
            item(key = "albums_header") {
                SectionHeader(title = stringResource(R.string.search_section_albums))
            }
            items(
                items = albums,
                key = { "album_${it.id}" }
            ) { album ->
                AlbumResultItem(
                    album = album,
                    query = query,
                    loadCoverArt = stateHolder::loadCoverArt,
                    getCachedCoverArt = stateHolder::getCachedCoverArt,
                    onClick = { onAlbumClick(album.id) }
                )
            }
            item(key = "albums_spacer") {
                Spacer(modifier = Modifier.height(SectionBottomSpacing))
            }
        }

        if (showSongs) {
            item(key = "songs_header") {
                SectionHeader(title = stringResource(R.string.search_section_songs))
            }
            items(
                items = songs,
                key = { "song_${it.id}" }
            ) { song ->
                SongResultItem(
                    song = song,
                    query = query,
                    loadCoverArt = stateHolder::loadCoverArt,
                    getCachedCoverArt = stateHolder::getCachedCoverArt,
                    onClick = { song.albumId?.let { onAlbumClick(it) } }
                )
            }
        }
    }
}

/**
 * Section header for a group of search results.
 */
@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(
            horizontal = SectionTitlePadding,
            vertical = ChipSpacing
        )
    )
}

/**
 * Artist result item with circular thumbnail and highlighted name.
 */
@Composable
private fun ArtistResultItem(
    artist: SearchArtistViewObject,
    query: String,
    loadCoverArt: suspend (String, Int) -> ImageBitmap?,
    getCachedCoverArt: (String) -> ImageBitmap?,
    modifier: Modifier = Modifier
) {
    var coverArt by remember(artist.coverArtId) {
        mutableStateOf(artist.coverArtId?.let { getCachedCoverArt(it) })
    }
    val density = LocalDensity.current

    LaunchedEffect(artist.coverArtId) {
        if (coverArt == null) {
            artist.coverArtId?.let { id ->
                val sizePx = with(density) { ThumbnailSize.roundToPx() }
                coverArt = loadCoverArt(id, sizePx)
            }
        }
    }

    ListItem(
        headlineContent = {
            HighlightedText(
                text = artist.name,
                query = query,
                maxLines = 1
            )
        },
        supportingContent = {
            Text(
                text = pluralStringResource(
                    R.plurals.search_artist_album_count,
                    artist.albumCount,
                    artist.albumCount
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            ResultThumbnail(
                coverArt = coverArt,
                shape = CircleShape
            )
        },
        modifier = modifier
    )
}

/**
 * Album result item with rounded rect thumbnail and highlighted metadata.
 */
@Composable
private fun AlbumResultItem(
    album: SearchAlbumViewObject,
    query: String,
    loadCoverArt: suspend (String, Int) -> ImageBitmap?,
    getCachedCoverArt: (String) -> ImageBitmap?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var coverArt by remember(album.coverArtId) {
        mutableStateOf(album.coverArtId?.let { getCachedCoverArt(it) })
    }
    val density = LocalDensity.current

    LaunchedEffect(album.coverArtId) {
        if (coverArt == null) {
            album.coverArtId?.let { id ->
                val sizePx = with(density) { ThumbnailSize.roundToPx() }
                coverArt = loadCoverArt(id, sizePx)
            }
        }
    }

    val trackCountText = album.songCount?.let { count ->
        pluralStringResource(R.plurals.search_album_track_count, count, count)
    }
    val metaText = buildString {
        append(album.artistName)
        trackCountText?.let { append(METADATA_SEPARATOR).append(it) }
        album.year?.let { append(METADATA_SEPARATOR).append(it) }
    }

    ListItem(
        headlineContent = {
            HighlightedText(
                text = album.name,
                query = query,
                maxLines = 1
            )
        },
        supportingContent = {
            HighlightedText(
                text = metaText,
                query = query,
                maxLines = 1
            )
        },
        leadingContent = {
            ResultThumbnail(
                coverArt = coverArt,
                shape = RoundedCornerShape(ThumbnailCornerRadius)
            )
        },
        modifier = modifier.clickable(onClick = onClick)
    )
}

/**
 * Song result item with rounded rect thumbnail and highlighted metadata.
 */
@Composable
private fun SongResultItem(
    song: SearchSongViewObject,
    query: String,
    loadCoverArt: suspend (String, Int) -> ImageBitmap?,
    getCachedCoverArt: (String) -> ImageBitmap?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var coverArt by remember(song.coverArtId) {
        mutableStateOf(song.coverArtId?.let { getCachedCoverArt(it) })
    }
    val density = LocalDensity.current

    LaunchedEffect(song.coverArtId) {
        if (coverArt == null) {
            song.coverArtId?.let { id ->
                val sizePx = with(density) { ThumbnailSize.roundToPx() }
                coverArt = loadCoverArt(id, sizePx)
            }
        }
    }

    ListItem(
        headlineContent = {
            HighlightedText(
                text = song.title,
                query = query,
                maxLines = 1
            )
        },
        supportingContent = {
            HighlightedText(
                text = song.artistName + METADATA_SEPARATOR + song.albumName,
                query = query,
                maxLines = 1
            )
        },
        leadingContent = {
            ResultThumbnail(
                coverArt = coverArt,
                shape = RoundedCornerShape(ThumbnailCornerRadius)
            )
        },
        modifier = modifier.clickable(onClick = onClick)
    )
}

/**
 * Cover art thumbnail with placeholder fallback.
 */
@Composable
private fun ResultThumbnail(
    coverArt: ImageBitmap?,
    shape: Shape,
    modifier: Modifier = Modifier
) {
    if (coverArt != null) {
        Image(
            bitmap = coverArt,
            contentDescription = null,
            modifier = modifier
                .size(ThumbnailSize)
                .clip(shape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier
                .size(ThumbnailSize)
                .clip(shape)
        ) {
            CoverArtPlaceholder()
        }
    }
}

/**
 * Displays text with matching portions of the [query] highlighted
 * in the primary color.
 */
@Composable
private fun HighlightedText(
    text: String,
    query: String,
    maxLines: Int,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val annotated = remember(text, query, primaryColor) {
        buildHighlightedString(text, query, primaryColor)
    }

    Text(
        text = annotated,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

/**
 * Builds an [AnnotatedString] with case-insensitive query matches
 * highlighted using the given [highlightColor].
 */
private fun buildHighlightedString(
    text: String,
    query: String,
    highlightColor: Color
) = buildAnnotatedString {
    if (query.isBlank()) {
        append(text)
        return@buildAnnotatedString
    }

    val lowerText = text.lowercase()
    val lowerQuery = query.lowercase()
    var currentIndex = 0

    while (currentIndex < text.length) {
        val matchIndex = lowerText.indexOf(lowerQuery, currentIndex)
        if (matchIndex == -1) {
            append(text.substring(currentIndex))
            break
        }

        if (matchIndex > currentIndex) {
            append(text.substring(currentIndex, matchIndex))
        }

        withStyle(SpanStyle(color = highlightColor)) {
            append(text.substring(matchIndex, matchIndex + query.length))
        }

        currentIndex = matchIndex + query.length
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyResultsContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.search_no_results),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorResultsContent(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.search_error),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (message.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
