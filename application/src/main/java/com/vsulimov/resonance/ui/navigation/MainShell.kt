package com.vsulimov.resonance.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vsulimov.resonance.core.ui.getAppContainer
import com.vsulimov.resonance.ui.components.MiniPlayer
import com.vsulimov.resonance.ui.screens.details.album.AlbumDetailsScreen
import com.vsulimov.resonance.ui.screens.details.artist.ArtistDetailsScreen
import com.vsulimov.resonance.ui.screens.details.playlist.PlaylistDetailsScreen
import com.vsulimov.resonance.ui.screens.downloads.DownloadsScreen
import com.vsulimov.resonance.ui.screens.downloads.ManageDownloadsScreen
import com.vsulimov.resonance.ui.screens.home.HomeScreen
import com.vsulimov.resonance.ui.screens.library.LibraryScreen
import com.vsulimov.resonance.ui.screens.library.categories.albums.AlbumsScreen
import com.vsulimov.resonance.ui.screens.library.categories.artists.ArtistsScreen
import com.vsulimov.resonance.ui.screens.library.categories.favorites.FavoritesScreen
import com.vsulimov.resonance.ui.screens.library.categories.playlists.PlaylistsScreen
import com.vsulimov.resonance.ui.screens.nowplaying.NowPlayingScreen
import com.vsulimov.resonance.ui.screens.search.SearchScreen
import com.vsulimov.resonance.ui.screens.settings.SettingsScreen
import com.vsulimov.resonance.ui.theme.Motion
import kotlinx.coroutines.launch

/**
 * The main application shell that wraps the BottomNavigationBar and the core navigation graph.
 *
 * Uses an Edge-to-Edge aware Scaffold.
 *
 * @param modifier The modifier to be applied to the layout.
 * @param onLogout Callback triggered when user signs out from Settings screen.
 */
@Composable
fun MainShell(modifier: Modifier = Modifier, onLogout: () -> Unit = {}) {
    val navController = rememberNavController()
    var homeScrollToTopTrigger by remember { mutableIntStateOf(0) }
    var isNowPlayingExpanded by rememberSaveable { mutableStateOf(false) }

    val appContainer = getAppContainer()
    val playerController = appContainer.playerController
    val favoritesRepository = appContainer.favoritesRepository

    val playbackStateState = playerController.playbackState.collectAsStateWithLifecycle()
    val currentSong by remember { androidx.compose.runtime.derivedStateOf { playbackStateState.value.currentSong } }
    val isPlaying by remember { androidx.compose.runtime.derivedStateOf { playbackStateState.value.isPlaying } }
    val isBuffering by remember { androidx.compose.runtime.derivedStateOf { playbackStateState.value.isBuffering } }
    val queue by remember { androidx.compose.runtime.derivedStateOf { playbackStateState.value.queue } }
    val queueIndex by remember { androidx.compose.runtime.derivedStateOf { playbackStateState.value.queueIndex } }
    val progressFractionProvider = remember {
        {
            val duration = playbackStateState.value.durationMs
            if (duration > 0) {
                (playbackStateState.value.currentPositionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
        }
    }

    val starredSongIds by favoritesRepository.starredSongIds.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        favoritesRepository.loadFavorites()
    }

    LaunchedEffect(currentSong) {
        if (currentSong == null) {
            isNowPlayingExpanded = false
        }
    }

    if (isNowPlayingExpanded) {
        BackHandler {
            isNowPlayingExpanded = false
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val currentRoute = currentDestination?.route

            var lastIsBottomBarVisible by rememberSaveable { mutableStateOf(true) }
            val isBottomBarVisible = remember(currentRoute) {
                if (currentRoute == null) {
                    lastIsBottomBarVisible
                } else {
                    val visible = Screen.bottomNavItems.any { it.route == currentRoute }
                    lastIsBottomBarVisible = visible
                    visible
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                AnimatedVisibility(
                    visible = currentSong != null,
                    enter = slideInVertically(
                        initialOffsetY = { fullHeight -> fullHeight },
                        animationSpec = tween(
                            durationMillis = Motion.DURATION_MEDIUM,
                            easing = Motion.EASING_ENTER
                        )
                    ) + fadeIn(
                        animationSpec = tween(
                            durationMillis = Motion.DURATION_MEDIUM,
                            easing = Motion.EASING_ENTER
                        )
                    ),
                    exit = slideOutVertically(
                        targetOffsetY = { fullHeight -> fullHeight },
                        animationSpec = tween(
                            durationMillis = Motion.DURATION_MEDIUM,
                            easing = Motion.EASING_EXIT
                        )
                    ) + fadeOut(
                        animationSpec = tween(
                            durationMillis = Motion.DURATION_MEDIUM,
                            easing = Motion.EASING_EXIT
                        )
                    )
                ) {
                    currentSong?.let { song ->
                        MiniPlayer(
                            song = song,
                            isPlaying = isPlaying,
                            isBuffering = isBuffering,
                            progressFractionProvider = progressFractionProvider,
                            onPlayPauseClick = playerController::togglePlayPause,
                            onClick = { isNowPlayingExpanded = true },
                            queue = queue,
                            queueIndex = queueIndex,
                            starredSongIds = starredSongIds,
                            onToggleFavorite = { targetSong ->
                                coroutineScope.launch {
                                    favoritesRepository.toggleSongFavorite(targetSong.id)
                                }
                            },
                            onSkipToQueueIndex = playerController::skipToQueueIndex
                        )
                    }
                }

                AnimatedVisibility(
                    visible = isBottomBarVisible,
                    enter = expandVertically(
                        expandFrom = Alignment.Bottom,
                        animationSpec = tween(
                            durationMillis = Motion.DURATION_MEDIUM,
                            easing = Motion.EASING_ENTER
                        )
                    ) + fadeIn(
                        animationSpec = tween(
                            durationMillis = Motion.DURATION_MEDIUM,
                            easing = Motion.EASING_ENTER
                        )
                    ),
                    exit = shrinkVertically(
                        shrinkTowards = Alignment.Bottom,
                        animationSpec = tween(
                            durationMillis = Motion.DURATION_MEDIUM,
                            easing = Motion.EASING_EXIT
                        )
                    ) + fadeOut(
                        animationSpec = tween(
                            durationMillis = Motion.DURATION_MEDIUM,
                            easing = Motion.EASING_EXIT
                        )
                    )
                ) {
                    NavigationBar(
                        modifier = Modifier.fillMaxWidth(),
                        windowInsets = WindowInsets(0, 0, 0, 0)
                    ) {
                        Screen.bottomNavItems.forEach { screen ->
                            NavigationBarItem(
                                icon = { Icon(screen.icon, contentDescription = null) },
                                label = { Text(stringResource(screen.titleResId)) },
                                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                onClick = {
                                    if ((screen == Screen.Home) && (currentRoute == Screen.Home.route)) {
                                        homeScrollToTopTrigger++
                                    } else if ((screen == Screen.Library) &&
                                        (currentRoute?.startsWith("library/") == true)
                                    ) {
                                        navController.popBackStack(Screen.Library.route, inclusive = false)
                                    } else {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsBottomHeight(WindowInsets.navigationBars)
                        .background(MaterialTheme.colorScheme.surface)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { fadeIn(animationSpec = tween(Motion.DURATION_MEDIUM)) },
                exitTransition = { fadeOut(animationSpec = tween(Motion.DURATION_MEDIUM)) },
                popEnterTransition = { fadeIn(animationSpec = tween(Motion.DURATION_MEDIUM)) },
                popExitTransition = { fadeOut(animationSpec = tween(Motion.DURATION_MEDIUM)) }
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        innerPadding = innerPadding,
                        onProfileClick = { navController.navigate(Screen.Settings.route) },
                        onDownloadsClick = { navController.navigate(Screen.Downloads.route) },
                        onAlbumClick = { albumId -> navController.navigate(Screen.AlbumDetails.createRoute(albumId)) },
                        scrollToTopTrigger = homeScrollToTopTrigger
                    )
                }
                composable(
                    route = Screen.Library.route,
                    exitTransition = {
                        if (targetState.destination.route?.startsWith("library/") == true) {
                            slideOutOfContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(Motion.DURATION_MEDIUM)
                            ) + fadeOut(animationSpec = tween(Motion.DURATION_MEDIUM))
                        } else {
                            fadeOut(animationSpec = tween(Motion.DURATION_MEDIUM))
                        }
                    },
                    popEnterTransition = {
                        if (initialState.destination.route?.startsWith("library/") == true) {
                            slideIntoContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(Motion.DURATION_MEDIUM)
                            ) + fadeIn(animationSpec = tween(Motion.DURATION_MEDIUM))
                        } else {
                            fadeIn(animationSpec = tween(Motion.DURATION_MEDIUM))
                        }
                    }
                ) {
                    LibraryScreen(
                        innerPadding = innerPadding,
                        onCategoryClick = { route -> navController.navigate(route) }
                    )
                }
                composable(Screen.Search.route) {
                    SearchScreen(
                        innerPadding = innerPadding,
                        onAlbumClick = { albumId -> navController.navigate(Screen.AlbumDetails.createRoute(albumId)) },
                        onArtistClick = { artistId ->
                            navController.navigate(Screen.ArtistDetails.createRoute(artistId))
                        }
                    )
                }

                composable(
                    route = Screen.Settings.route,
                    enterTransition = { slideInLeft() },
                    exitTransition = { slideOutLeft() },
                    popEnterTransition = { slideInRight() },
                    popExitTransition = { slideOutRight() }
                ) {
                    SettingsScreen(
                        innerPadding = innerPadding,
                        onBackClick = { navController.popBackStack() },
                        onLoggedOut = onLogout
                    )
                }

                composable(
                    route = Screen.Downloads.route,
                    enterTransition = { slideInLeft() },
                    exitTransition = { slideOutLeft() },
                    popEnterTransition = { slideInRight() },
                    popExitTransition = { slideOutRight() }
                ) {
                    DownloadsScreen(
                        innerPadding = innerPadding,
                        onBackClick = { navController.popBackStack() },
                        onManageDownloadsClick = { navController.navigate(Screen.ManageDownloads.route) }
                    )
                }

                composable(
                    route = Screen.ManageDownloads.route,
                    enterTransition = { slideInLeft() },
                    exitTransition = { slideOutLeft() },
                    popEnterTransition = { slideInRight() },
                    popExitTransition = { slideOutRight() }
                ) {
                    ManageDownloadsScreen(
                        innerPadding = innerPadding,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.Albums.route,
                    enterTransition = { slideInLeft() },
                    exitTransition = { slideOutLeft() },
                    popEnterTransition = { slideInRight() },
                    popExitTransition = { slideOutRight() }
                ) {
                    AlbumsScreen(
                        innerPadding = innerPadding,
                        onBackClick = { navController.popBackStack() },
                        onAlbumClick = { albumId -> navController.navigate(Screen.AlbumDetails.createRoute(albumId)) }
                    )
                }

                composable(
                    route = Screen.Artists.route,
                    enterTransition = { slideInLeft() },
                    exitTransition = { slideOutLeft() },
                    popEnterTransition = { slideInRight() },
                    popExitTransition = { slideOutRight() }
                ) {
                    ArtistsScreen(
                        innerPadding = innerPadding,
                        onBackClick = { navController.popBackStack() },
                        onArtistClick = { artistId ->
                            navController.navigate(Screen.ArtistDetails.createRoute(artistId))
                        }
                    )
                }

                composable(
                    route = Screen.Playlists.route,
                    enterTransition = { slideInLeft() },
                    exitTransition = { slideOutLeft() },
                    popEnterTransition = { slideInRight() },
                    popExitTransition = { slideOutRight() }
                ) {
                    PlaylistsScreen(
                        innerPadding = innerPadding,
                        onBackClick = { navController.popBackStack() },
                        onPlaylistClick = { playlistId ->
                            navController.navigate(Screen.PlaylistDetails.createRoute(playlistId))
                        }
                    )
                }

                composable(
                    route = Screen.Favorites.route,
                    enterTransition = { slideInLeft() },
                    exitTransition = { slideOutLeft() },
                    popEnterTransition = { slideInRight() },
                    popExitTransition = { slideOutRight() }
                ) {
                    FavoritesScreen(
                        innerPadding = innerPadding,
                        onBackClick = { navController.popBackStack() },
                        onAlbumClick = { albumId -> navController.navigate(Screen.AlbumDetails.createRoute(albumId)) },
                        onArtistClick = { artistId ->
                            navController.navigate(Screen.ArtistDetails.createRoute(artistId))
                        }
                    )
                }

                composable(
                    route = Screen.AlbumDetails.route,
                    arguments = listOf(navArgument(Screen.AlbumDetails.ARG_ALBUM_ID) { type = NavType.StringType }),
                    enterTransition = { slideInLeft() },
                    exitTransition = { slideOutLeft() },
                    popEnterTransition = { slideInRight() },
                    popExitTransition = { slideOutRight() }
                ) { backStackEntry ->
                    val albumId = backStackEntry.arguments?.getString(Screen.AlbumDetails.ARG_ALBUM_ID) ?: ""
                    AlbumDetailsScreen(
                        albumId = albumId,
                        innerPadding = innerPadding,
                        onBackClick = { navController.popBackStack() },
                        onArtistClick = { artistId ->
                            navController.navigate(Screen.ArtistDetails.createRoute(artistId))
                        }
                    )
                }

                composable(
                    route = Screen.ArtistDetails.route,
                    arguments = listOf(navArgument(Screen.ArtistDetails.ARG_ARTIST_ID) { type = NavType.StringType }),
                    enterTransition = { slideInLeft() },
                    exitTransition = { slideOutLeft() },
                    popEnterTransition = { slideInRight() },
                    popExitTransition = { slideOutRight() }
                ) { backStackEntry ->
                    val artistId = backStackEntry.arguments?.getString(Screen.ArtistDetails.ARG_ARTIST_ID) ?: ""
                    ArtistDetailsScreen(
                        artistId = artistId,
                        innerPadding = innerPadding,
                        onBackClick = { navController.popBackStack() },
                        onAlbumClick = { albumId -> navController.navigate(Screen.AlbumDetails.createRoute(albumId)) }
                    )
                }

                composable(
                    route = Screen.PlaylistDetails.route,
                    arguments = listOf(
                        navArgument(Screen.PlaylistDetails.ARG_PLAYLIST_ID) {
                            type = NavType.StringType
                        }
                    ),
                    enterTransition = { slideInLeft() },
                    exitTransition = { slideOutLeft() },
                    popEnterTransition = { slideInRight() },
                    popExitTransition = { slideOutRight() }
                ) { backStackEntry ->
                    val playlistId = backStackEntry.arguments?.getString(Screen.PlaylistDetails.ARG_PLAYLIST_ID) ?: ""
                    PlaylistDetailsScreen(
                        playlistId = playlistId,
                        innerPadding = innerPadding,
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }

            if (isNowPlayingExpanded && currentSong != null) {
                NowPlayingScreenWrapper(
                    playbackStateState = playbackStateState,
                    starredSongIds = starredSongIds,
                    onDismiss = { isNowPlayingExpanded = false },
                    playerController = playerController,
                    favoritesRepository = favoritesRepository,
                    onArtistClick = { artistId ->
                        isNowPlayingExpanded = false
                        navController.navigate(Screen.ArtistDetails.createRoute(artistId))
                    }
                )
            }
        }
    }
}

@Composable
private fun NowPlayingScreenWrapper(
    playbackStateState: androidx.compose.runtime.State<com.vsulimov.resonance.player.model.PlaybackState>,
    starredSongIds: Set<String>,
    onDismiss: () -> Unit,
    playerController: com.vsulimov.resonance.player.PlayerController,
    favoritesRepository: com.vsulimov.resonance.domain.repository.FavoritesRepository,
    onArtistClick: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    NowPlayingScreen(
        playbackStateProvider = { playbackStateState.value },
        onDismiss = onDismiss,
        onPlayPauseClick = playerController::togglePlayPause,
        onSkipNextClick = playerController::skipToNext,
        onSkipPreviousClick = playerController::skipToPrevious,
        onSeekTo = playerController::seekTo,
        onToggleShuffle = playerController::toggleShuffle,
        onToggleRepeat = playerController::toggleRepeat,
        onRemoveFromQueue = playerController::removeFromQueue,
        onClearQueue = playerController::clearQueue,
        onPlayQueueItem = playerController::skipToQueueIndex,
        starredSongIds = starredSongIds,
        onToggleFavorite = { targetSong ->
            coroutineScope.launch {
                favoritesRepository.toggleSongFavorite(targetSong.id)
            }
        },
        onArtistClick = onArtistClick
    )
}

/** Helper extension creating slide in from left + fade in animation spec. */
private fun AnimatedContentTransitionScope<*>.slideInLeft() = slideIntoContainer(
    towards = AnimatedContentTransitionScope.SlideDirection.Left,
    animationSpec = tween(
        durationMillis = Motion.DURATION_MEDIUM,
        easing = Motion.EASING_EMPHASIZED
    )
) + fadeIn(
    animationSpec = tween(
        durationMillis = Motion.DURATION_MEDIUM,
        easing = Motion.EASING_ENTER
    )
)

/** Helper extension creating slide out to left + fade out animation spec. */
private fun AnimatedContentTransitionScope<*>.slideOutLeft() = slideOutOfContainer(
    towards = AnimatedContentTransitionScope.SlideDirection.Left,
    animationSpec = tween(
        durationMillis = Motion.DURATION_MEDIUM,
        easing = Motion.EASING_EMPHASIZED
    )
) + fadeOut(
    animationSpec = tween(
        durationMillis = Motion.DURATION_MEDIUM,
        easing = Motion.EASING_EXIT
    )
)

/** Helper extension creating slide in from right + fade in animation spec. */
private fun AnimatedContentTransitionScope<*>.slideInRight() = slideIntoContainer(
    towards = AnimatedContentTransitionScope.SlideDirection.Right,
    animationSpec = tween(
        durationMillis = Motion.DURATION_MEDIUM,
        easing = Motion.EASING_EMPHASIZED
    )
) + fadeIn(
    animationSpec = tween(
        durationMillis = Motion.DURATION_MEDIUM,
        easing = Motion.EASING_ENTER
    )
)

/** Helper extension creating slide out to right + fade out animation spec. */
private fun AnimatedContentTransitionScope<*>.slideOutRight() = slideOutOfContainer(
    towards = AnimatedContentTransitionScope.SlideDirection.Right,
    animationSpec = tween(
        durationMillis = Motion.DURATION_MEDIUM,
        easing = Motion.EASING_EMPHASIZED
    )
) + fadeOut(
    animationSpec = tween(
        durationMillis = Motion.DURATION_MEDIUM,
        easing = Motion.EASING_EXIT
    )
)
