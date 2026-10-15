package com.vsulimov.resonance.core.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale

/**
 * A Composable that asynchronously loads and displays cover art from Subsonic API.
 * Uses [com.vsulimov.resonance.core.imageloader.SubsonicImageLoader] from the [AppContainer].
 *
 * @param coverArtId The ID of the cover art from the Subsonic API.
 * @param modifier The modifier to be applied to the layout.
 * @param size An optional size hint for the server (e.g., to request a thumbnail).
 * @param contentDescription Text used by accessibility services.
 */
@Composable
fun CoverImage(
    coverArtId: String,
    modifier: Modifier = Modifier,
    size: Int? = null,
    contentDescription: String? = null
) {
    val appContainer = getAppContainer()
    var bitmap by remember(coverArtId, size) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(coverArtId, size) { mutableStateOf(true) }

    LaunchedEffect(coverArtId, size) {
        isLoading = true
        bitmap = appContainer.imageLoader.loadCoverArt(coverArtId, size)
        isLoading = false
    }

    val currentBitmap = bitmap
    if (currentBitmap != null) {
        Image(
            bitmap = currentBitmap.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        // Placeholder or Error State
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Album,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxSize(0.5f) // Icon takes half the size of the placeholder
            )
        }
    }
}
