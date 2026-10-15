package com.vsulimov.resonance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vsulimov.resonance.ResonanceApplication
import com.vsulimov.resonance.ui.screens.auth.AuthScreen
import com.vsulimov.resonance.ui.screens.serverconfig.ServerConfigScreen
import com.vsulimov.resonance.ui.screens.welcome.WelcomeScreen
import com.vsulimov.resonance.ui.theme.Motion
import com.vsulimov.resonance.ui.theme.ResonanceTheme

/** Route names for the onboarding flow. */
private object Routes {
    const val WELCOME = "welcome"
    const val SERVER_CONFIG = "server_config"
    const val AUTH = "auth/{serverUrl}"
    const val HOME = "home"

    fun auth(serverUrl: String) =
        "auth/${java.net.URLEncoder.encode(serverUrl, java.nio.charset.StandardCharsets.UTF_8.name())}"
}

/**
 * Main entry point of the Resonance application.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as ResonanceApplication).container
        val startRoute = if (appContainer.credentialsRepository.hasCredentials()) {
            Routes.HOME
        } else {
            Routes.WELCOME
        }

        setContent {
            ResonanceTheme {
                val navController = rememberNavController()

                androidx.compose.material3.Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = startRoute,
                        enterTransition = {
                            slideIntoContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(Motion.DURATION_MEDIUM)
                            ) + fadeIn(animationSpec = tween(Motion.DURATION_MEDIUM))
                        },
                        exitTransition = {
                            slideOutOfContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(Motion.DURATION_MEDIUM)
                            ) + fadeOut(animationSpec = tween(Motion.DURATION_MEDIUM))
                        },
                        popEnterTransition = {
                            slideIntoContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(Motion.DURATION_MEDIUM)
                            ) + fadeIn(animationSpec = tween(Motion.DURATION_MEDIUM))
                        },
                        popExitTransition = {
                            slideOutOfContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(Motion.DURATION_MEDIUM)
                            ) + fadeOut(animationSpec = tween(Motion.DURATION_MEDIUM))
                        }
                    ) {
                        composable(route = Routes.WELCOME) {
                            WelcomeScreen(
                                onGetStartedClick = { navController.navigate(Routes.SERVER_CONFIG) }
                            )
                        }
                        composable(route = Routes.SERVER_CONFIG) {
                            ServerConfigScreen(
                                onContinue = { url -> navController.navigate(Routes.auth(url)) },
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                        composable(
                            route = Routes.AUTH,
                            arguments = listOf(navArgument("serverUrl") { type = NavType.StringType })
                        ) {
                            AuthScreen(
                                onBackClick = { navController.popBackStack() },
                                onAuthSuccess = {
                                    navController.navigate(Routes.HOME) {
                                        popUpTo(0)
                                    }
                                }
                            )
                        }
                        composable(route = Routes.HOME) {
                            com.vsulimov.resonance.ui.navigation.MainShell()
                        }
                    }
                }
            }
        }
    }
}
