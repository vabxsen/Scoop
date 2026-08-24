package com.scoop.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.scoop.app.core.model.CookieSite
import com.scoop.app.ui.screen.downloaddetails.DownloadDetailsScreen
import com.scoop.app.ui.screen.downloads.DownloadsScreen
import com.scoop.app.ui.screen.home.HomeScreen
import com.scoop.app.ui.screen.settings.SettingsAboutScreen
import com.scoop.app.ui.screen.settings.SettingsCreditsScreen
import com.scoop.app.ui.screen.settings.SettingsDownloadsScreen
import com.scoop.app.ui.screen.settings.SettingsGeneralScreen
import com.scoop.app.ui.screen.settings.SettingsHubScreen
import com.scoop.app.ui.screen.settings.cookies.CookiesScreen
import com.scoop.app.ui.theme.Motion

private val enterFromEnd: AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> EnterTransition = {
    slideInHorizontally(animationSpec = tween(Motion.EMPHASIZED_MS), initialOffsetX = { it / 3 }) +
        fadeIn(animationSpec = tween(Motion.EMPHASIZED_MS))
}

private val exitToStart: AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> ExitTransition = {
    slideOutHorizontally(animationSpec = tween(Motion.EMPHASIZED_MS), targetOffsetX = { -it / 5 }) +
        fadeOut(animationSpec = tween(Motion.STANDARD_MS))
}

private val enterFromStart: AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> EnterTransition = {
    slideInHorizontally(animationSpec = tween(Motion.EMPHASIZED_MS), initialOffsetX = { -it / 3 }) +
        fadeIn(animationSpec = tween(Motion.EMPHASIZED_MS))
}

private val exitToEnd: AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> ExitTransition = {
    slideOutHorizontally(animationSpec = tween(Motion.EMPHASIZED_MS), targetOffsetX = { it / 5 }) +
        fadeOut(animationSpec = tween(Motion.STANDARD_MS))
}

/**
 * Home is the single root/hub (matching the reference app's model): Settings and Downloads are
 * reached via icons on Home and pushed as normal back-stack destinations, not bottom-nav tabs.
 */
@Composable
fun ScoopNavHost(startUrl: String? = null) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Route.HOME) {
        composable(Route.HOME) {
            HomeScreen(
                startUrl = startUrl,
                onOpenDownloads = { navController.navigate(Route.DOWNLOADS) },
                onOpenSettings = { navController.navigate(Route.SETTINGS_HUB) },
            )
        }

        composable(Route.DOWNLOADS, enterTransition = enterFromEnd, exitTransition = exitToStart, popEnterTransition = enterFromStart, popExitTransition = exitToEnd) {
            DownloadsScreen(
                onBack = { navController.popBackStack() },
                onOpenDownload = { taskId -> navController.navigate(Route.downloadDetails(taskId)) },
            )
        }

        composable(Route.SETTINGS_HUB, enterTransition = enterFromEnd, exitTransition = exitToStart, popEnterTransition = enterFromStart, popExitTransition = exitToEnd) {
            SettingsHubScreen(
                onBack = { navController.popBackStack() },
                onOpenGeneral = { navController.navigate(Route.SETTINGS_GENERAL) },
                onOpenDownloads = { navController.navigate(Route.SETTINGS_DOWNLOADS) },
                onOpenAbout = { navController.navigate(Route.SETTINGS_ABOUT) },
            )
        }
        composable(Route.SETTINGS_GENERAL, enterTransition = enterFromEnd, exitTransition = exitToStart, popEnterTransition = enterFromStart, popExitTransition = exitToEnd) {
            SettingsGeneralScreen(onBack = { navController.popBackStack() })
        }
        composable(Route.SETTINGS_DOWNLOADS, enterTransition = enterFromEnd, exitTransition = exitToStart, popEnterTransition = enterFromStart, popExitTransition = exitToEnd) {
            SettingsDownloadsScreen(
                onBack = { navController.popBackStack() },
                onOpenCookies = { site -> navController.navigate(Route.cookies(site.name)) },
            )
        }
        composable(Route.SETTINGS_ABOUT, enterTransition = enterFromEnd, exitTransition = exitToStart, popEnterTransition = enterFromStart, popExitTransition = exitToEnd) {
            SettingsAboutScreen(
                onBack = { navController.popBackStack() },
                onOpenCredits = { navController.navigate(Route.SETTINGS_CREDITS) },
            )
        }
        composable(Route.SETTINGS_CREDITS, enterTransition = enterFromEnd, exitTransition = exitToStart, popEnterTransition = enterFromStart, popExitTransition = exitToEnd) {
            SettingsCreditsScreen(onBack = { navController.popBackStack() })
        }

        composable(Route.COOKIES, enterTransition = enterFromEnd, exitTransition = exitToStart, popEnterTransition = enterFromStart, popExitTransition = exitToEnd) { backStack ->
            val site = CookieSite.entries.firstOrNull { it.name == backStack.arguments?.getString(Route.COOKIES_ARG) } ?: CookieSite.YOUTUBE
            CookiesScreen(site = site, onDone = { navController.popBackStack() })
        }

        composable(
            route = Route.DOWNLOAD_DETAILS,
            enterTransition = enterFromEnd,
            exitTransition = exitToStart,
            popEnterTransition = enterFromStart,
            popExitTransition = exitToEnd,
        ) { backStack ->
            val taskId = backStack.arguments?.getString(Route.DOWNLOAD_DETAILS_ARG).orEmpty()
            DownloadDetailsScreen(taskId = taskId, onBack = { navController.popBackStack() })
        }
    }
}
