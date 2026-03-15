package com.vsulimov.resonance.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vsulimov.resonance.ResonanceApplication
import com.vsulimov.resonance.domain.model.AlbumSortType
import com.vsulimov.resonance.ui.screen.albumdetail.AlbumDetailScreen
import com.vsulimov.resonance.ui.screen.albumdetail.AlbumDetailViewModel
import com.vsulimov.resonance.ui.screen.albumlist.AlbumListScreen
import com.vsulimov.resonance.ui.screen.albumlist.AlbumListViewModel
import com.vsulimov.resonance.ui.screen.artistlist.ArtistListScreen
import com.vsulimov.resonance.ui.screen.main.MainShellScreen
import com.vsulimov.resonance.ui.screen.settings.SettingsScreen
import com.vsulimov.resonance.ui.screen.settings.cache.CacheScreen
import com.vsulimov.resonance.ui.screen.songlist.SongListScreen

/**
 * Top-level route definitions for the application.
 *
 * The app has three major sections:
 * - [Onboarding] — Welcome and server login screens shown to unauthenticated users.
 * - [Main] — The main application shell with bottom navigation, shown after login.
 * - [AlbumDetail] — Full-screen album detail with track listing.
 *
 * @param route Navigation route string for this section.
 */
sealed class AppRoute(val route: String) {

    /** Onboarding flow (Welcome → Server Login). */
    data object Onboarding : AppRoute("onboarding")

    /** Main application shell with tabs. */
    data object Main : AppRoute("main")

    /** Album detail screen. Route template includes `{albumId}` argument. */
    data object AlbumDetail : AppRoute("album/{${AlbumDetailViewModel.ALBUM_ID_ARG}}") {

        /** Builds a concrete route for the given [albumId]. */
        fun createRoute(albumId: String): String = "album/$albumId"
    }

    /** Artists list screen accessed from the Library tab. */
    data object ArtistList : AppRoute("artists")

    /** Albums list screen with sort type parameter. */
    data object AlbumList : AppRoute("albums/{${AlbumListViewModel.SORT_TYPE_ARG}}") {

        /** Builds a concrete route for the given [sortType]. */
        fun createRoute(sortType: AlbumSortType): String = "albums/${sortType.name}"
    }

    /** Songs list screen accessed from the Library tab. */
    data object SongList : AppRoute("songs")

    /** Settings screen accessed from the avatar button. */
    data object Settings : AppRoute("settings")

    /** Cache management screen accessed from Settings. */
    data object Cache : AppRoute("settings/cache")
}

/** Duration for forward/backward depth transitions (M3 standard). */
private const val DEPTH_TRANSITION_DURATION_MS = 300

/** Duration for the fade portion of depth transitions. */
private const val DEPTH_FADE_DURATION_MS = 150

/** Scale factor for the incoming screen during forward navigation. */
private const val DEPTH_INITIAL_SCALE = 0.92f

/** Scale factor for the outgoing screen during forward navigation. */
private const val DEPTH_TARGET_SCALE = 1.05f

/**
 * Forward enter: incoming screen fades in and scales up from [DEPTH_INITIAL_SCALE].
 */
private fun depthEnterTransition(): EnterTransition =
    fadeIn(
        animationSpec = tween(
            durationMillis = DEPTH_TRANSITION_DURATION_MS,
            easing = LinearOutSlowInEasing
        )
    ) + scaleIn(
        initialScale = DEPTH_INITIAL_SCALE,
        animationSpec = tween(
            durationMillis = DEPTH_TRANSITION_DURATION_MS,
            easing = LinearOutSlowInEasing
        )
    )

/**
 * Forward exit: outgoing screen scales up to [DEPTH_TARGET_SCALE] without fading,
 * so no blank background is exposed while the incoming screen fades in.
 */
private fun depthExitTransition(): ExitTransition =
    scaleOut(
        targetScale = DEPTH_TARGET_SCALE,
        animationSpec = tween(
            durationMillis = DEPTH_TRANSITION_DURATION_MS,
            easing = FastOutLinearInEasing
        )
    )

/**
 * Pop enter: the uncovered screen scales back from [DEPTH_TARGET_SCALE] without fading,
 * since it was never faded out.
 */
private fun depthPopEnterTransition(): EnterTransition =
    scaleIn(
        initialScale = DEPTH_TARGET_SCALE,
        animationSpec = tween(
            durationMillis = DEPTH_TRANSITION_DURATION_MS,
            easing = LinearOutSlowInEasing
        )
    )

/**
 * Pop exit: the leaving screen fades out and scales down to [DEPTH_INITIAL_SCALE].
 */
private fun depthPopExitTransition(): ExitTransition =
    fadeOut(
        animationSpec = tween(
            durationMillis = DEPTH_FADE_DURATION_MS,
            easing = FastOutLinearInEasing
        )
    ) + scaleOut(
        targetScale = DEPTH_INITIAL_SCALE,
        animationSpec = tween(
            durationMillis = DEPTH_TRANSITION_DURATION_MS,
            easing = FastOutLinearInEasing
        )
    )

/**
 * Root navigation composable for the Resonance application.
 *
 * Determines the start destination by checking for stored credentials:
 * - If credentials exist, navigates directly to the main shell.
 * - If no credentials are found, starts the onboarding flow.
 *
 * Observes the global [SessionStateHolder][com.vsulimov.resonance.core.SessionStateHolder]
 * for authentication failures. When the session expires (e.g. the server rejects
 * stored credentials), all navigation state is cleared and the user is redirected
 * to onboarding.
 *
 * @param modifier Modifier applied to the root [NavHost].
 */
@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val application = LocalContext.current.applicationContext as ResonanceApplication
    val container = application.appContainer

    var startDestination by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (startDestination != null) return@LaunchedEffect
        val hasCredentials = try {
            container.loadCredentialsUseCase() != null
        } catch (_: Exception) {
            false
        }
        startDestination = if (hasCredentials) {
            AppRoute.Main.route
        } else {
            AppRoute.Onboarding.route
        }
    }

    val destination = startDestination ?: return

    val navController = rememberNavController()

    LaunchedEffect(Unit) {
        container.sessionStateHolder.sessionExpired.collect {
            navController.navigate(AppRoute.Onboarding.route) {
                popUpTo(AppRoute.Main.route) { inclusive = true }
            }
        }
    }

    AppNavHost(
        navController = navController,
        startDestination = destination,
        modifier = modifier
    )
}

/**
 * Navigation host containing the onboarding graph and the main shell destination.
 *
 * @param navController Controller managing top-level navigation state.
 * @param startDestination Initial route — either [AppRoute.Onboarding] or [AppRoute.Main].
 * @param modifier Modifier applied to the [NavHost].
 */
@Composable
private fun AppNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        onboardingGraph(
            navController = navController,
            route = AppRoute.Onboarding.route,
            onOnboardingComplete = {
                navController.navigate(AppRoute.Main.route) {
                    popUpTo(AppRoute.Onboarding.route) { inclusive = true }
                }
            }
        )

        composable(
            route = AppRoute.Main.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None }
        ) {
            MainShellScreen(
                onAlbumClick = { albumId ->
                    navController.navigate(AppRoute.AlbumDetail.createRoute(albumId))
                },
                onSettingsClick = {
                    navController.navigate(AppRoute.Settings.route)
                },
                onArtistsClick = {
                    navController.navigate(AppRoute.ArtistList.route)
                },
                onAlbumsClick = {
                    navController.navigate(
                        AppRoute.AlbumList.createRoute(AlbumSortType.ALPHABETICAL)
                    )
                },
                onSongsClick = {
                    navController.navigate(AppRoute.SongList.route)
                },
                onSeeAllClick = { sortType ->
                    navController.navigate(AppRoute.AlbumList.createRoute(sortType))
                }
            )
        }

        composable(
            route = AppRoute.Settings.route,
            enterTransition = { depthEnterTransition() },
            exitTransition = { depthExitTransition() },
            popEnterTransition = { depthPopEnterTransition() },
            popExitTransition = { depthPopExitTransition() }
        ) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToCache = {
                    navController.navigate(AppRoute.Cache.route)
                }
            )
        }

        composable(
            route = AppRoute.Cache.route,
            enterTransition = { depthEnterTransition() },
            exitTransition = { depthExitTransition() },
            popEnterTransition = { depthPopEnterTransition() },
            popExitTransition = { depthPopExitTransition() }
        ) {
            CacheScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = AppRoute.ArtistList.route,
            enterTransition = { depthEnterTransition() },
            exitTransition = { depthExitTransition() },
            popEnterTransition = { depthPopEnterTransition() },
            popExitTransition = { depthPopExitTransition() }
        ) {
            ArtistListScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = AppRoute.SongList.route,
            enterTransition = { depthEnterTransition() },
            exitTransition = { depthExitTransition() },
            popEnterTransition = { depthPopEnterTransition() },
            popExitTransition = { depthPopExitTransition() }
        ) {
            SongListScreen(
                onBack = { navController.popBackStack() },
                onAlbumClick = { albumId ->
                    navController.navigate(AppRoute.AlbumDetail.createRoute(albumId))
                }
            )
        }

        composable(
            route = AppRoute.AlbumList.route,
            arguments = listOf(
                navArgument(AlbumListViewModel.SORT_TYPE_ARG) {
                    type = NavType.StringType
                }
            ),
            enterTransition = { depthEnterTransition() },
            exitTransition = { depthExitTransition() },
            popEnterTransition = { depthPopEnterTransition() },
            popExitTransition = { depthPopExitTransition() }
        ) { backStackEntry ->
            val sortTypeName = requireNotNull(
                backStackEntry.arguments?.getString(AlbumListViewModel.SORT_TYPE_ARG)
            )
            AlbumListScreen(
                sortType = AlbumSortType.valueOf(sortTypeName),
                onBack = { navController.popBackStack() },
                onAlbumClick = { albumId ->
                    navController.navigate(AppRoute.AlbumDetail.createRoute(albumId))
                }
            )
        }

        composable(
            route = AppRoute.AlbumDetail.route,
            arguments = listOf(
                navArgument(AlbumDetailViewModel.ALBUM_ID_ARG) {
                    type = NavType.StringType
                }
            ),
            enterTransition = { depthEnterTransition() },
            exitTransition = { depthExitTransition() },
            popEnterTransition = { depthPopEnterTransition() },
            popExitTransition = { depthPopExitTransition() }
        ) {
            AlbumDetailScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
