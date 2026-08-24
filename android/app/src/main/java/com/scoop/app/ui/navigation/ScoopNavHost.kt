package com.scoop.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.scoop.app.R
import com.scoop.app.ui.screen.downloads.DownloadsScreen
import com.scoop.app.ui.screen.home.HomeScreen
import com.scoop.app.ui.screen.settings.SettingsScreen

private data class TopLevelDestination(val route: String, val labelRes: Int, val icon: ImageVector)

private val topLevelDestinations =
    listOf(
        TopLevelDestination(Route.HOME, R.string.nav_home, Icons.Outlined.Home),
        TopLevelDestination(Route.DOWNLOADS, R.string.nav_downloads, Icons.Outlined.Download),
        TopLevelDestination(Route.SETTINGS, R.string.nav_settings, Icons.Outlined.Settings),
    )

@Composable
fun ScoopNavHost(startUrl: String? = null) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route
            NavigationBar {
                topLevelDestinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(stringResource(destination.labelRes)) },
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Route.HOME,
            modifier = androidx.compose.ui.Modifier.padding(innerPadding),
        ) {
            composable(Route.HOME) { HomeScreen(startUrl = startUrl) }
            composable(Route.DOWNLOADS) { DownloadsScreen() }
            composable(Route.SETTINGS) { SettingsScreen() }
        }
    }
}
