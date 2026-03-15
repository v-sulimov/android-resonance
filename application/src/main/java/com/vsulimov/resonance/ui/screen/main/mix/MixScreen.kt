package com.vsulimov.resonance.ui.screen.main.mix

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsulimov.resonance.R
import com.vsulimov.resonance.domain.model.AlbumSortType
import com.vsulimov.resonance.ui.component.AlbumCarousel
import com.vsulimov.resonance.ui.component.ErrorContent

private val HeroHorizontalPadding = 16.dp
private val HeroTopPadding = 4.dp
private val HeroBottomPadding = 20.dp
private val CarouselBottomSpacing = 24.dp

/**
 * Mix tab screen displaying a shuffle hero card and album carousels.
 *
 * Shows four horizontally scrollable album sections: Recently played,
 * Most played, Recently added, and Random picks. Each section loads
 * independently — individual failures are silently excluded while
 * successful sections display normally.
 *
 * State is scoped to [MixViewModel] so that carousel data, loading
 * state, and cover art survive configuration changes.
 *
 * @param innerPadding Padding provided by the parent [Scaffold][androidx.compose.material3.Scaffold].
 * @param onAlbumClick Callback invoked when an album card is tapped,
 *   receiving the album's unique identifier.
 * @param onSeeAllClick Callback invoked when "See all" is tapped on a carousel,
 *   receiving the [AlbumSortType] for the target list screen.
 * @param modifier Modifier applied to the root layout.
 */
@Composable
fun MixScreen(
    innerPadding: PaddingValues,
    onAlbumClick: (albumId: String) -> Unit,
    onSeeAllClick: (AlbumSortType) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: MixViewModel = viewModel(factory = MixViewModel.Factory)
    val stateHolder = viewModel.stateHolder
    val screenState by stateHolder.screenState.collectAsState()

    when (val state = screenState) {
        is MixScreenState.Loading -> LoadingContent(
            innerPadding = innerPadding,
            modifier = modifier
        )
        is MixScreenState.Content -> MixContent(
            carousels = state.carousels,
            stateHolder = stateHolder,
            onAlbumClick = onAlbumClick,
            onSeeAllClick = onSeeAllClick,
            innerPadding = innerPadding,
            modifier = modifier
        )
        is MixScreenState.Error -> ErrorContent(
            title = stringResource(R.string.mix_error_load_failed),
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
 * Main Mix tab content with hero card and album carousels.
 */
@Composable
private fun MixContent(
    carousels: List<AlbumCarouselViewObject>,
    stateHolder: MixStateHolder,
    onAlbumClick: (albumId: String) -> Unit,
    onSeeAllClick: (AlbumSortType) -> Unit,
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        item(key = "hero") {
            ShuffleHeroCard(
                onShuffleClick = { /* TODO: Shuffle all tracks */ },
                modifier = Modifier
                    .padding(
                        start = HeroHorizontalPadding,
                        end = HeroHorizontalPadding,
                        top = HeroTopPadding,
                        bottom = HeroBottomPadding
                    )
            )
        }

        items(
            items = carousels,
            key = { it.carouselType.name }
        ) { carousel ->
            AlbumCarousel(
                carousel = carousel,
                loadCoverArt = stateHolder::loadCoverArt,
                getCachedCoverArt = stateHolder::getCachedCoverArt,
                onActionClick = {
                    when (carousel.carouselType) {
                        CarouselType.RANDOM_PICKS -> stateHolder.refreshRandomPicks()
                        CarouselType.RECENTLY_PLAYED -> onSeeAllClick(AlbumSortType.RECENTLY_PLAYED)
                        CarouselType.MOST_PLAYED -> onSeeAllClick(AlbumSortType.MOST_PLAYED)
                        CarouselType.RECENTLY_ADDED -> onSeeAllClick(AlbumSortType.RECENTLY_ADDED)
                    }
                },
                onAlbumClick = { album -> onAlbumClick(album.id) }
            )
            Spacer(modifier = Modifier.height(CarouselBottomSpacing))
        }
    }
}
