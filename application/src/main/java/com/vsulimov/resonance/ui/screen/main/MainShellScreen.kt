package com.vsulimov.resonance.ui.screen.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsulimov.resonance.domain.model.AlbumSortType
import com.vsulimov.resonance.ui.component.MiniPlayerPlaceholder
import com.vsulimov.resonance.ui.navigation.MainTab
import com.vsulimov.resonance.ui.screen.main.library.LibraryScreen
import com.vsulimov.resonance.ui.screen.main.mix.MixScreen
import com.vsulimov.resonance.ui.screen.main.search.SearchScreen

private val AvatarSize = 32.dp

/**
 * Main application shell displayed after successful authentication.
 *
 * Hosts a Material 3 [Scaffold] with:
 * - **Top app bar** — displays the current tab title and a clickable user avatar.
 * - **Bottom navigation** — three tabs: Mix, Library, and Search.
 * - **Mini player placeholder** — structural slot above the navigation bar,
 *   invisible until playback is implemented.
 * - **Tab content** — placeholder screens for each tab.
 *
 * State is scoped to [MainShellViewModel] so that the selected tab and avatar
 * survive configuration changes.
 *
 * @param onAlbumClick Callback invoked when an album card is tapped,
 *   receiving the album's unique identifier.
 * @param onSettingsClick Callback invoked when the avatar button is tapped
 *   to navigate to the settings screen.
 * @param onArtistsClick Callback invoked when the Artists category card is tapped
 *   on the Library tab, navigating to the full artists list.
 * @param onAlbumsClick Callback invoked when the Albums category card is tapped
 *   on the Library tab, navigating to the full albums list.
 * @param onSongsClick Callback invoked when the Songs category card is tapped
 *   on the Library tab, navigating to the full songs list.
 * @param onSeeAllClick Callback invoked when "See all" is tapped on a Mix tab
 *   carousel, receiving the [AlbumSortType] for the target list.
 * @param modifier Modifier applied to the root [Scaffold].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShellScreen(
    onAlbumClick: (albumId: String) -> Unit,
    onSettingsClick: () -> Unit,
    onArtistsClick: () -> Unit,
    onAlbumsClick: () -> Unit,
    onSongsClick: () -> Unit,
    onSeeAllClick: (AlbumSortType) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: MainShellViewModel = viewModel(factory = MainShellViewModel.Factory)
    val stateHolder = viewModel.stateHolder
    val selectedTab by stateHolder.selectedTab.collectAsState()
    val avatarInitial by stateHolder.avatarInitial.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            if (selectedTab != MainTab.SEARCH) {
                TopAppBar(
                    title = {
                        Text(text = stringResource(selectedTab.labelResId))
                    },
                    actions = {
                        AvatarButton(
                            initial = avatarInitial,
                            onClick = onSettingsClick
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )
            }
        },
        bottomBar = {
            Column {
                MiniPlayerPlaceholder()
                NavigationBar {
                    MainTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = tab == selectedTab,
                            onClick = { stateHolder.selectTab(tab) },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = stringResource(tab.labelResId)
                                )
                            },
                            label = { Text(text = stringResource(tab.labelResId)) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            MainTab.MIX -> MixScreen(
                innerPadding = innerPadding,
                onAlbumClick = onAlbumClick,
                onSeeAllClick = onSeeAllClick
            )
            MainTab.LIBRARY -> LibraryScreen(
                innerPadding = innerPadding,
                onArtistsClick = onArtistsClick,
                onAlbumsClick = onAlbumsClick,
                onSongsClick = onSongsClick
            )
            MainTab.SEARCH -> SearchScreen(
                innerPadding = innerPadding,
                onAlbumClick = onAlbumClick
            )
        }
    }
}

/**
 * Circular avatar button displaying the user's initial.
 *
 * Uses [MaterialTheme.colorScheme.primaryContainer] as background and
 * [MaterialTheme.colorScheme.onPrimaryContainer] for the text, following
 * Material 3 color role guidelines.
 *
 * @param initial Single character to display (typically the first letter of the username).
 * @param onClick Callback invoked when the avatar is tapped.
 * @param modifier Modifier applied to the [IconButton].
 */
@Composable
private fun AvatarButton(
    initial: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Box(
            modifier = Modifier
                .size(AvatarSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
