package com.vsulimov.resonance.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsulimov.libsubsonic.data.response.browsing.Album
import com.vsulimov.resonance.R
import com.vsulimov.resonance.core.ui.CoverImage
import com.vsulimov.resonance.ui.theme.Dimens

@Composable
fun HomeScreen(modifier: Modifier = Modifier, viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)) {
    val uiState by viewModel.uiState.collectAsState()
    val timeOfDay = viewModel.getGreetingTimeOfDay()

    if (uiState.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp + Dimens.paddingLarge)
    ) {
        // Top Bar: Greeting & Profile
        item {
            HomeTopBar(
                username = uiState.username,
                timeOfDay = timeOfDay,
                onProfileClick = { /* TODO: Navigate to settings */ }
            )
        }

        // Hero Card: Shuffle All
        item {
            HeroActionCard(
                onShuffleClick = viewModel::shuffleAll,
                modifier = Modifier.padding(horizontal = Dimens.paddingLarge, vertical = Dimens.paddingMedium)
            )
        }

        // Discovery Carousels
        if (uiState.recentlyPlayed.isNotEmpty()) {
            item {
                AlbumCarousel(
                    title = stringResource(R.string.home_section_recently_played),
                    albums = uiState.recentlyPlayed,
                    onAlbumClick = { /* TODO: Navigate to album */ }
                )
            }
        }

        if (uiState.recentlyAdded.isNotEmpty()) {
            item {
                AlbumCarousel(
                    title = stringResource(R.string.home_section_recently_added),
                    albums = uiState.recentlyAdded,
                    onAlbumClick = { /* TODO: Navigate to album */ }
                )
            }
        }

        if (uiState.mostPlayed.isNotEmpty()) {
            item {
                AlbumCarousel(
                    title = stringResource(R.string.home_section_most_played),
                    albums = uiState.mostPlayed,
                    onAlbumClick = { /* TODO: Navigate to album */ }
                )
            }
        }

        if (uiState.randomAlbums.isNotEmpty()) {
            item {
                AlbumCarousel(
                    title = stringResource(R.string.home_section_random),
                    albums = uiState.randomAlbums,
                    onAlbumClick = { /* TODO: Navigate to album */ }
                )
            }
        }
    }
}

@Composable
private fun HomeTopBar(
    username: String,
    timeOfDay: TimeOfDay,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val greetingResId = when (timeOfDay) {
        TimeOfDay.MORNING -> R.string.home_greeting_morning
        TimeOfDay.AFTERNOON -> R.string.home_greeting_afternoon
        TimeOfDay.EVENING -> R.string.home_greeting_evening
        TimeOfDay.NIGHT -> R.string.home_greeting_night
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = Dimens.paddingLarge,
                end = Dimens.paddingSmall, // IconButton has its own padding
                top = Dimens.paddingSmall,
                bottom = Dimens.paddingSmall
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(greetingResId, username),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        IconButton(onClick = onProfileClick) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = stringResource(R.string.cd_profile_icon),
                modifier = Modifier.size(Dimens.iconLarge),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HeroActionCard(onShuffleClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.paddingLarge),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_hero_subtitle),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(Dimens.paddingSmall))
                Text(
                    text = stringResource(R.string.home_hero_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = onShuffleClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(horizontal = Dimens.paddingLarge, vertical = Dimens.paddingMedium)
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.iconMedium)
                )
                Spacer(modifier = Modifier.width(Dimens.paddingSmall))
                Text(
                    text = stringResource(R.string.home_btn_shuffle_all),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AlbumCarousel(
    title: String,
    albums: List<Album>,
    onAlbumClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(vertical = Dimens.paddingMedium)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = Dimens.paddingLarge)
        )

        Spacer(modifier = Modifier.height(Dimens.paddingMedium))

        LazyRow(
            contentPadding = PaddingValues(horizontal = Dimens.paddingLarge),
            horizontalArrangement = Arrangement.spacedBy(Dimens.paddingMedium)
        ) {
            items(albums, key = { it.id }) { album ->
                AlbumItem(
                    album = album,
                    onClick = { onAlbumClick(album.id) },
                    modifier = Modifier.width(140.dp) // Fixed width for carousel items
                )
            }
        }
    }
}

@Composable
private fun AlbumItem(album: Album, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            // We set a fixed height for the entire AlbumItem to completely eliminate UI jitter.
            // Width: 140dp. Image is 1:1 (140dp). Spacing is 8dp.
            // Title + Artist text typically take ~40dp. We set 200dp total.
            .height(200.dp)
            .clickable(onClick = onClick)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            shape = MaterialTheme.shapes.medium
        ) {
            val coverId = album.coverArt
            if (coverId != null) {
                CoverImage(
                    coverArtId = coverId,
                    size = 300, // Request a reasonable thumbnail size
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Fallback if no cover art is available
                Box(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Album,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(Dimens.iconExtraLarge)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Dimens.paddingSmall))

        Text(
            text = album.name ?: stringResource(R.string.home_unknown_album),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Unconditionally render the artist text (using a blank space if null or empty)
        // to ensure the item height remains exactly the same for all albums.
        // This prevents the LazyRow from changing height and causing UI jitter during horizontal scroll.
        Text(
            text = album.artist?.takeIf { it.isNotBlank() } ?: " ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
