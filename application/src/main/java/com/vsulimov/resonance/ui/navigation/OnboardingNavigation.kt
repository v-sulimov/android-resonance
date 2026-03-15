package com.vsulimov.resonance.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.vsulimov.resonance.ui.screen.onboarding.login.ServerLoginScreen
import com.vsulimov.resonance.ui.screen.onboarding.welcome.WelcomeScreen

/** Route definitions for the onboarding flow. */
sealed class OnboardingRoute(val route: String) {
    data object Welcome : OnboardingRoute("onboarding/welcome")
    data object ServerLogin : OnboardingRoute("onboarding/server-login")
}

private const val TRANSITION_DURATION_MS = 300

/**
 * Onboarding navigation graph containing Welcome and Server Login screens.
 *
 * Uses shared-axis-X transitions (slide left to advance, slide right to go back).
 * This is a [NavGraphBuilder] extension intended to be nested inside a parent
 * [NavHost][androidx.navigation.compose.NavHost] via [AppNavigation].
 *
 * @param navController Controller managing navigation state (shared with parent).
 * @param route Route string for this nested graph (typically [AppRoute.Onboarding]).
 * @param onOnboardingComplete Callback invoked when login succeeds and the user
 *   taps "Continue".
 */
fun NavGraphBuilder.onboardingGraph(
    navController: NavHostController,
    route: String,
    onOnboardingComplete: () -> Unit
) {
    navigation(
        startDestination = OnboardingRoute.Welcome.route,
        route = route
    ) {
        composable(
            route = OnboardingRoute.Welcome.route,
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(TRANSITION_DURATION_MS)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(TRANSITION_DURATION_MS)
                )
            }
        ) {
            WelcomeScreen(
                onGetStartedClick = {
                    navController.navigate(OnboardingRoute.ServerLogin.route)
                }
            )
        }

        composable(
            route = OnboardingRoute.ServerLogin.route,
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(TRANSITION_DURATION_MS)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(TRANSITION_DURATION_MS)
                )
            }
        ) {
            ServerLoginScreen(
                onLoginSuccess = onOnboardingComplete
            )
        }
    }
}
