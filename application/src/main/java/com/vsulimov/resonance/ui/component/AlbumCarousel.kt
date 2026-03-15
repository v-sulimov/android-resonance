package com.vsulimov.resonance.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vsulimov.resonance.ui.screen.main.mix.AlbumCardViewObject
import com.vsulimov.resonance.ui.screen.main.mix.AlbumCarouselViewObject

private val SectionHeaderHorizontalPadding = 16.dp
private val SectionHeaderEndPadding = 4.dp
private val SectionHeaderBottomPadding = 12.dp
private val ItemSpacing = 12.dp
private val RowHorizontalPadding = 16.dp

/**
 * Reusable horizontal album carousel section with a header.
 *
 * Displays a section title, an action button (e.g. "See all" or "Refresh"),
 * and a horizontally scrollable row of [AlbumCard] items.
 *
 * @param carousel View object containing section metadata and album list.
 * @param loadCoverArt Suspend function passed through to each [AlbumCard]
 *   for asynchronous cover art loading.
 * @param getCachedCoverArt Synchronous lookup passed through to each [AlbumCard]
 *   for instant display of previously loaded cover art.
 * @param onActionClick Callback invoked when the header action button is tapped.
 * @param onAlbumClick Callback invoked when an album card is tapped,
 *   receiving the tapped [AlbumCardViewObject].
 * @param modifier Modifier applied to the root layout.
 */
@Composable
fun AlbumCarousel(
    carousel: AlbumCarouselViewObject,
    loadCoverArt: suspend (coverArtId: String, sizePx: Int) -> ImageBitmap?,
    getCachedCoverArt: (coverArtId: String) -> ImageBitmap?,
    onActionClick: () -> Unit,
    onAlbumClick: (AlbumCardViewObject) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = SectionHeaderHorizontalPadding,
                    end = SectionHeaderEndPadding,
                    bottom = SectionHeaderBottomPadding
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(carousel.titleResId),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            TextButton(onClick = onActionClick) {
                Text(
                    text = stringResource(carousel.actionLabelResId),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = RowHorizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(ItemSpacing)
        ) {
            items(
                items = carousel.albums,
                key = { it.id }
            ) { album ->
                AlbumCard(
                    album = album,
                    loadCoverArt = loadCoverArt,
                    getCachedCoverArt = getCachedCoverArt,
                    onClick = { onAlbumClick(album) }
                )
            }
        }
    }
}
