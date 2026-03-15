package com.vsulimov.resonance.ui.screen.main.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsulimov.resonance.R
import com.vsulimov.resonance.ui.component.ErrorContent

private val GridHorizontalPadding = 16.dp
private val GridTopPadding = 8.dp
private val GridBottomPadding = 24.dp
private val GridSpacing = 12.dp
private val CardCornerRadius = 16.dp
private val CardVerticalPadding = 20.dp
private val CardHorizontalPadding = 16.dp
private val IconContainerSize = 40.dp
private val IconContainerCornerRadius = 12.dp
private val IconSize = 24.dp
private val IconToLabelSpacing = 12.dp

/**
 * Library tab screen displaying a grid of category cards with item counts.
 *
 * Shows six categories in a two-column grid: Artists, Albums, Songs,
 * Playlists, Genres, and Favorites. Each card displays an icon, category
 * name, and the total item count fetched from the server.
 *
 * State is scoped to [LibraryViewModel] so that category counts and
 * loading state survive configuration changes.
 *
 * @param innerPadding Padding provided by the parent [Scaffold][androidx.compose.material3.Scaffold].
 * @param onArtistsClick Callback invoked when the Artists category card is tapped.
 * @param onAlbumsClick Callback invoked when the Albums category card is tapped.
 * @param onSongsClick Callback invoked when the Songs category card is tapped.
 * @param modifier Modifier applied to the root layout.
 */
@Composable
fun LibraryScreen(
    innerPadding: PaddingValues,
    onArtistsClick: () -> Unit,
    onAlbumsClick: () -> Unit,
    onSongsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.Factory)
    val stateHolder = viewModel.stateHolder
    val screenState by stateHolder.screenState.collectAsState()

    when (val state = screenState) {
        is LibraryScreenState.Loading -> LoadingContent(
            innerPadding = innerPadding,
            modifier = modifier
        )
        is LibraryScreenState.Content -> LibraryContent(
            categories = state.categories,
            onCategoryClick = { category ->
                when (category) {
                    LibraryCategory.ARTISTS -> onArtistsClick()
                    LibraryCategory.ALBUMS -> onAlbumsClick()
                    LibraryCategory.SONGS -> onSongsClick()
                    else -> { /* TODO: Navigate to other category screens */ }
                }
            },
            innerPadding = innerPadding,
            modifier = modifier
        )
        is LibraryScreenState.Error -> ErrorContent(
            title = stringResource(R.string.library_error_load_failed),
            onRetry = stateHolder::retry,
            subtitle = state.message.takeIf { it.isNotEmpty() },
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}

/**
 * Centered loading indicator shown during initial data fetch.
 */
@Composable
private fun LoadingContent(
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(innerPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
    }
}

/**
 * Two-column grid of category cards.
 */
@Composable
private fun LibraryContent(
    categories: List<LibraryCategoryCardViewObject>,
    onCategoryClick: (LibraryCategory) -> Unit,
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(
            start = GridHorizontalPadding,
            end = GridHorizontalPadding,
            top = GridTopPadding,
            bottom = GridBottomPadding
        ),
        horizontalArrangement = Arrangement.spacedBy(GridSpacing),
        verticalArrangement = Arrangement.spacedBy(GridSpacing),
        modifier = modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        items(
            items = categories,
            key = { it.category.name }
        ) { card ->
            CategoryCard(
                card = card,
                onClick = { onCategoryClick(card.category) }
            )
        }
    }
}

/**
 * Single category card displaying an icon, label, and optional count.
 *
 * The Favorites category uses [MaterialTheme.colorScheme.errorContainer]
 * for its icon background and [MaterialTheme.colorScheme.error] for the
 * icon tint, creating a distinct accent per the mockup design.
 */
@Composable
private fun CategoryCard(
    card: LibraryCategoryCardViewObject,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isFavorite = card.category == LibraryCategory.FAVORITES
    val iconBackground = if (isFavorite) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val iconTint = if (isFavorite) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(CardCornerRadius),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = CardHorizontalPadding,
                vertical = CardVerticalPadding
            )
        ) {
            Box(
                modifier = Modifier
                    .size(IconContainerSize)
                    .clip(RoundedCornerShape(IconContainerCornerRadius))
                    .background(iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = card.category.icon,
                    contentDescription = null,
                    modifier = Modifier.size(IconSize),
                    tint = iconTint
                )
            }
            Spacer(modifier = Modifier.height(IconToLabelSpacing))
            Text(
                text = stringResource(card.category.labelResId),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (card.count != null) {
                Text(
                    text = pluralStringResource(
                        card.category.countPluralResId,
                        card.count,
                        card.count
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Returns the Material icon for a [LibraryCategory].
 */
private val LibraryCategory.icon: ImageVector
    get() = when (this) {
        LibraryCategory.ARTISTS -> Icons.Outlined.PersonAdd
        LibraryCategory.ALBUMS -> Icons.Outlined.GridView
        LibraryCategory.SONGS -> Icons.Outlined.MusicNote
        LibraryCategory.PLAYLISTS -> Icons.Outlined.QueueMusic
        LibraryCategory.GENRES -> Icons.Outlined.Album
        LibraryCategory.FAVORITES -> Icons.Filled.Favorite
    }

/**
 * Returns the label string resource ID for a [LibraryCategory].
 */
private val LibraryCategory.labelResId: Int
    get() = when (this) {
        LibraryCategory.ARTISTS -> R.string.library_artists
        LibraryCategory.ALBUMS -> R.string.library_albums
        LibraryCategory.SONGS -> R.string.library_songs
        LibraryCategory.PLAYLISTS -> R.string.library_playlists
        LibraryCategory.GENRES -> R.string.library_genres
        LibraryCategory.FAVORITES -> R.string.library_favorites
    }

/**
 * Returns the count plural string resource ID for a [LibraryCategory].
 */
private val LibraryCategory.countPluralResId: Int
    get() = when (this) {
        LibraryCategory.ARTISTS -> R.plurals.library_artist_count
        LibraryCategory.ALBUMS -> R.plurals.library_album_count
        LibraryCategory.SONGS -> R.plurals.library_song_count
        LibraryCategory.PLAYLISTS -> R.plurals.library_playlist_count
        LibraryCategory.GENRES -> R.plurals.library_genre_count
        LibraryCategory.FAVORITES -> R.plurals.library_favorite_count
    }
