package com.vsulimov.resonance

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vsulimov.resonance.data.settings.AppThemeMode
import com.vsulimov.resonance.ui.navigation.MainShell
import com.vsulimov.resonance.ui.screens.auth.AuthScreen
import com.vsulimov.resonance.ui.screens.notifications.NotificationPermissionScreen
import com.vsulimov.resonance.ui.screens.serverconfig.ServerConfigScreen
import com.vsulimov.resonance.ui.screens.welcome.WelcomeScreen
import com.vsulimov.resonance.ui.theme.Motion
import com.vsulimov.resonance.ui.theme.ResonanceTheme

/** Route names for the onboarding flow. */
private object Routes {
    /** Route to the welcome screen. */
    const val WELCOME = "welcome"

    /** Route to the notification permission rationale screen. */
    const val NOTIFICATION_PERMISSION = "notification_permission"

    /** Route to the server configuration screen. */
    const val SERVER_CONFIG = "server_config"

    /** Base route to the authentication screen, requires a URL argument. */
    const val AUTH = "auth/{serverUrl}"

    /** Route to the main logged-in shell of the app. */
    const val HOME = "home"

    /**
     * Encodes server URL parameter and returns navigation route for Auth screen.
     *
     * @param serverUrl The URL of the Subsonic server.
     * @return The formatted navigation route string.
     */
    fun auth(serverUrl: String) =
        "auth/${java.net.URLEncoder.encode(serverUrl, java.nio.charset.StandardCharsets.UTF_8.name())}"
}

/**
 * Main entry point of the Resonance application.
 */
class MainActivity : ComponentActivity() {
    /**
     * Called when the activity is starting. Configures Edge-to-Edge window insets,
     * theme states, and the main onboarding/home navigation graph.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down
     *                           then this Bundle contains the data it most recently supplied.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        enableEdgeToEdge()

        val appContainer = (application as ResonanceApplication).container
        val startRoute = if (appContainer.credentialsRepository.hasCredentials()) {
            Routes.HOME
        } else {
            Routes.WELCOME
        }

        setContent {
            val themeMode by appContainer.settingsRepository.themeModeState.collectAsStateWithLifecycle()
            val isDynamicColorEnabled by appContainer.settingsRepository
                .isDynamicColorEnabledState
                .collectAsStateWithLifecycle()

            val isDarkTheme = when (themeMode) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            DisposableEffect(isDarkTheme, isDynamicColorEnabled) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDarkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    },
                    navigationBarStyle = if (isDarkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    }
                )
                onDispose { }
            }

            val navController = rememberNavController()

            ResonanceTheme(
                darkTheme = isDarkTheme,
                dynamicColor = isDynamicColorEnabled
            ) {
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
                        },
                        exitTransition = {
                            slideOutOfContainer(
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
                        },
                        popEnterTransition = {
                            slideIntoContainer(
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
                        },
                        popExitTransition = {
                            slideOutOfContainer(
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
                        }
                    ) {
                        composable(route = Routes.WELCOME) {
                            WelcomeScreen(
                                onGetStartedClick = {
                                    val needsNotificationPermission =
                                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                            ContextCompat.checkSelfPermission(
                                                this@MainActivity,
                                                Manifest.permission.POST_NOTIFICATIONS
                                            ) != PackageManager.PERMISSION_GRANTED

                                    if (needsNotificationPermission) {
                                        navController.navigate(Routes.NOTIFICATION_PERMISSION)
                                    } else {
                                        navController.navigate(Routes.SERVER_CONFIG)
                                    }
                                }
                            )
                        }
                        composable(route = Routes.NOTIFICATION_PERMISSION) {
                            NotificationPermissionScreen(
                                onContinue = {
                                    navController.navigate(Routes.SERVER_CONFIG) {
                                        popUpTo(Routes.NOTIFICATION_PERMISSION) {
                                            inclusive = true
                                        }
                                    }
                                },
                                onBackClick = { navController.popBackStack() }
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
                            MainShell(
                                onLogout = {
                                    navController.navigate(Routes.WELCOME) {
                                        popUpTo(0)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
