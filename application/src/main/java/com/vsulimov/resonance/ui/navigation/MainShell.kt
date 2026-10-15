package com.vsulimov.resonance.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vsulimov.resonance.ui.screens.home.HomeScreen
import com.vsulimov.resonance.ui.screens.library.LibraryScreen
import com.vsulimov.resonance.ui.screens.search.SearchScreen
import com.vsulimov.resonance.ui.theme.Motion

/**
 * The main application shell that wraps the BottomNavigationBar and the core navigation graph.
 *
 * Uses an Edge-to-Edge aware Scaffold.
 */
@Composable
fun MainShell(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            NavigationBar(
                modifier = Modifier.fillMaxWidth()
            ) {
                Screen.bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(stringResource(screen.titleResId)) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                composable(Screen.Home.route) { HomeScreen() }
                composable(Screen.Library.route) { LibraryScreen() }
                composable(Screen.Search.route) { SearchScreen() }
            }
        }
    }
}
