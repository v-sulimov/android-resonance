package com.vsulimov.resonance.ui.screen.albumdetail

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsulimov.resonance.R
import com.vsulimov.resonance.ui.component.CoverArtPlaceholder
import com.vsulimov.resonance.ui.component.ErrorContent

// region Dimension constants (8dp grid)

private val CoverArtSize = 260.dp
private val CoverArtCornerRadius = 16.dp
private val CoverArtElevation = 8.dp
private val ContentHorizontalPadding = 24.dp
private val CoverArtTopPadding = 8.dp
private val CoverArtBottomSpacing = 24.dp
private val TitleToArtistSpacing = 4.dp
private val ArtistToMetadataSpacing = 4.dp
private val MetadataToActionsSpacing = 24.dp
private val ActionButtonSpacing = 12.dp
private val ActionIconSize = 20.dp
private val ActionIconSpacing = 8.dp
private val ActionsToListSpacing = 24.dp
private val TrackNumberMinWidth = 24.dp
private val TrackListBottomPadding = 16.dp
// endregion

/**
 * Album detail screen displaying album metadata and track listing.
 *
 * Follows Material 3 detail screen patterns:
 * - Transparent top app bar with back navigation overlaying content
 * - Centered cover art with elevation shadow and crossfade loading
 * - Album title (titleLarge), artist (titleSmall, primary), and metadata
 * - Play/Shuffle action buttons following M3 button hierarchy
 * - Track list with M3 list item dimensions (56dp min height) and ripple
 *
 * State is scoped to [AlbumDetailViewModel] so that data survives
 * configuration changes.
 *
 * @param onBack Callback invoked when the back navigation button is tapped.
 * @param modifier Modifier applied to the root layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: AlbumDetailViewModel = viewModel(factory = AlbumDetailViewModel.Factory)
    val stateHolder = viewModel.stateHolder
    val screenState by stateHolder.screenState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        when (val state = screenState) {
            is AlbumDetailScreenState.Loading -> LoadingContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
            is AlbumDetailScreenState.Content -> AlbumContent(
                album = state.album,
                stateHolder = stateHolder,
                innerPadding = innerPadding,
                modifier = Modifier.fillMaxSize()
            )
            is AlbumDetailScreenState.Error -> ErrorContent(
                title = stringResource(R.string.album_detail_error_load_failed),
                onRetry = stateHolder::retry,
                subtitle = state.message.takeIf { it.isNotEmpty() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}

/**
 * Centered loading indicator shown during initial data fetch.
 */
@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
    }
}

/**
 * Main album detail content with cover art, metadata, and track list.
 *
 * Uses [LazyColumn] for efficient scrolling of the track list. The
 * header section (cover art, info, actions) is rendered as lazy items
 * so they scroll together with the tracks.
 */
@Composable
private fun AlbumContent(
    album: AlbumDetailViewObject,
    stateHolder: AlbumDetailStateHolder,
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val layoutDirection = LocalLayoutDirection.current

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            top = innerPadding.calculateTopPadding(),
            bottom = innerPadding.calculateBottomPadding() + TrackListBottomPadding,
            start = innerPadding.calculateStartPadding(layoutDirection),
            end = innerPadding.calculateEndPadding(layoutDirection)
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item(key = "cover_art") {
            CoverArtSection(
                album = album,
                loadCoverArt = stateHolder::loadCoverArt
            )
        }

        item(key = "info") {
            AlbumInfoSection(album = album)
        }

        item(key = "actions") {
            ActionButtons(
                modifier = Modifier.padding(horizontal = ContentHorizontalPadding)
            )
            Spacer(modifier = Modifier.height(ActionsToListSpacing))
            HorizontalDivider()
        }

        itemsIndexed(
            items = album.tracks,
            key = { _, track -> track.id }
        ) { _, track ->
            TrackRow(
                track = track,
                onTrackClick = { /* TODO: Play track */ }
            )
        }
    }
}

/**
 * Cover art with elevation shadow and crossfade loading animation.
 *
 * Uses [AnimatedContent] to smoothly transition from the placeholder
 * to the loaded image, matching the pattern used by [AlbumCard][com.vsulimov.resonance.ui.component.AlbumCard].
 */
@Composable
private fun CoverArtSection(
    album: AlbumDetailViewObject,
    loadCoverArt: suspend (coverArtId: String, sizePx: Int) -> ImageBitmap?,
    modifier: Modifier = Modifier
) {
    var coverArt by remember(album.coverArtId) { mutableStateOf<ImageBitmap?>(null) }
    val density = LocalDensity.current

    LaunchedEffect(album.coverArtId) {
        album.coverArtId?.let { id ->
            val sizePx = with(density) { CoverArtSize.roundToPx() }
            coverArt = loadCoverArt(id, sizePx)
        }
    }

    val artShape = RoundedCornerShape(CoverArtCornerRadius)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = ContentHorizontalPadding,
                vertical = CoverArtTopPadding
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(CoverArtSize)
                .shadow(
                    elevation = CoverArtElevation,
                    shape = artShape
                )
                .clip(artShape)
        ) {
            AnimatedContent(
                targetState = coverArt,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                contentKey = { it != null },
                label = "detail_cover_art_crossfade"
            ) { bitmap ->
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = stringResource(R.string.cd_album_cover, album.name),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    CoverArtPlaceholder()
                }
            }
        }
    }
}

/** Separator between metadata parts (e.g. year, song count, duration). */
private const val METADATA_SEPARATOR = " \u00B7 "
private const val SECONDS_PER_MINUTE = 60
private const val SECONDS_PER_HOUR = 3600

/**
 * Album info section: title, artist, and metadata line.
 *
 * Typography follows M3 hierarchy:
 * - Title: [titleLarge] for prominence
 * - Artist: [titleSmall] in [primary] color for emphasis and future tappability
 * - Metadata: [bodySmall] in [onSurfaceVariant] for supporting information
 */
@Composable
private fun AlbumInfoSection(
    album: AlbumDetailViewObject,
    modifier: Modifier = Modifier
) {
    val metadataLine = buildMetadataLine(album)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = ContentHorizontalPadding,
                end = ContentHorizontalPadding,
                top = CoverArtBottomSpacing,
                bottom = MetadataToActionsSpacing
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = album.name,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(TitleToArtistSpacing))

        Text(
            text = album.artistName,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (metadataLine.isNotEmpty()) {
            Spacer(modifier = Modifier.height(ArtistToMetadataSpacing))
            Text(
                text = metadataLine,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Builds a localized metadata display line from structured album data.
 *
 * @param album The album view object with structured metadata fields.
 * @return Formatted metadata string (e.g. "2023 · 12 songs · 45 min"), or empty if no fields.
 */
@Composable
private fun buildMetadataLine(album: AlbumDetailViewObject): String {
    val parts = mutableListOf<String>()
    album.year?.let { parts.add(it.toString()) }
    album.songCount?.let { count ->
        parts.add(pluralStringResource(R.plurals.album_detail_song_count, count, count))
    }
    album.durationSeconds?.let { seconds ->
        val hours = seconds / SECONDS_PER_HOUR
        val minutes = (seconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
        val formatted = when {
            hours > 0 && minutes > 0 -> stringResource(
                R.string.album_detail_duration_hours_minutes,
                hours,
                minutes
            )
            hours > 0 -> stringResource(R.string.album_detail_duration_hours, hours)
            else -> stringResource(R.string.album_detail_duration_minutes, minutes)
        }
        parts.add(formatted)
    }
    return parts.joinToString(METADATA_SEPARATOR)
}

/**
 * Play and Shuffle action buttons following M3 button hierarchy.
 *
 * Play uses a filled [Button] (primary action), Shuffle uses a
 * [FilledTonalButton] (secondary action). Both fill available width
 * equally.
 */
@Composable
private fun ActionButtons(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = { /* TODO: Play album */ },
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Outlined.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(ActionIconSize)
            )
            Spacer(modifier = Modifier.size(ActionIconSpacing))
            Text(text = stringResource(R.string.album_detail_play))
        }

        Spacer(modifier = Modifier.width(ActionButtonSpacing))

        FilledTonalButton(
            onClick = { /* TODO: Shuffle album */ },
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Outlined.Shuffle,
                contentDescription = null,
                modifier = Modifier.size(ActionIconSize)
            )
            Spacer(modifier = Modifier.size(ActionIconSpacing))
            Text(text = stringResource(R.string.album_detail_shuffle))
        }
    }
}

/**
 * Single track row using M3 [ListItem] for spec-compliant spacing and layout.
 *
 * Layout follows M3 list item guidelines:
 * - Leading content: track number (right-aligned in a compact column)
 * - Headline: track title (single line, ellipsized)
 * - Trailing content: formatted duration and overflow menu icon
 *
 * All internal padding (16dp horizontal, 8dp vertical, 16dp leading gap)
 * is handled by [ListItem] per the M3 spec.
 *
 * @param track Track view object with pre-formatted display data.
 * @param onTrackClick Callback invoked when the track row is tapped.
 * @param modifier Modifier applied to the root layout.
 */
@Composable
private fun TrackRow(
    track: TrackViewObject,
    onTrackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        modifier = modifier.clickable(onClick = onTrackClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = {
            Text(
                text = track.trackNumber,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(TrackNumberMinWidth),
                textAlign = TextAlign.Center
            )
        },
        headlineContent = {
            Text(
                text = track.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (track.formattedDuration.isNotEmpty()) {
                    Text(
                        text = track.formattedDuration,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { /* TODO: Track overflow menu */ }) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = stringResource(R.string.cd_track_options, track.title),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    )
}
