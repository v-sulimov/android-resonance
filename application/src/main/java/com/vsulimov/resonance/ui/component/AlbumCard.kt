package com.vsulimov.resonance.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vsulimov.resonance.R
import com.vsulimov.resonance.ui.screen.main.mix.AlbumCardViewObject

/** Width and height of the album cover art image. */
val AlbumArtSize = 140.dp
private val ArtCornerRadius = 12.dp
private val ArtToNameSpacing = 8.dp
private val NameToArtistSpacing = 4.dp

/**
 * Reusable album card displaying cover art, album name, and artist name.
 *
 * Cover art is loaded asynchronously via the [loadCoverArt] suspend lambda.
 * Previously loaded images are retrieved synchronously via [getCachedCoverArt]
 * to avoid placeholder flashes when items are recycled during scrolling.
 * A crossfade transition is applied when cover art loads for the first time.
 *
 * @param album View object containing album display data.
 * @param loadCoverArt Suspend function that fetches cover art by ID and size.
 *   Called within a [LaunchedEffect] keyed on [AlbumCardViewObject.coverArtId].
 * @param getCachedCoverArt Synchronous lookup for previously loaded cover art.
 *   Used to initialize state without waiting for the suspend call.
 * @param onClick Callback invoked when the card is tapped.
 * @param modifier Modifier applied to the root layout.
 */
@Composable
fun AlbumCard(
    album: AlbumCardViewObject,
    loadCoverArt: suspend (coverArtId: String, sizePx: Int) -> ImageBitmap?,
    getCachedCoverArt: (coverArtId: String) -> ImageBitmap?,
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
                val sizePx = with(density) { AlbumArtSize.roundToPx() }
                coverArt = loadCoverArt(id, sizePx)
            }
        }
    }

    Column(
        modifier = modifier
            .width(AlbumArtSize)
            .clickable(onClick = onClick)
    ) {
        val artShape = RoundedCornerShape(ArtCornerRadius)

        AnimatedContent(
            targetState = coverArt,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            contentKey = { it != null },
            label = "cover_art_crossfade"
        ) { bitmap ->
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = stringResource(R.string.cd_album_cover, album.name),
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(artShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(artShape)
                ) {
                    CoverArtPlaceholder()
                }
            }
        }

        Spacer(modifier = Modifier.height(ArtToNameSpacing))

        Text(
            text = album.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(NameToArtistSpacing))

        Text(
            text = album.artistName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
