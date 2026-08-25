package com.scoop.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.scoop.app.ui.screen.downloaddetails.DownloadDetailsScreen
import com.scoop.app.ui.screen.downloads.DownloadsScreen
import com.scoop.app.ui.screen.home.HomeScreen
import com.scoop.app.ui.screen.settings.SettingsAboutScreen
import com.scoop.app.ui.screen.settings.SettingsCreditsScreen
import com.scoop.app.ui.screen.settings.SettingsDownloadsScreen
import com.scoop.app.ui.screen.settings.SettingsGeneralScreen
import com.scoop.app.ui.screen.settings.SettingsHubScreen
import com.scoop.app.ui.screen.settings.SettingsStorageScreen
import com.scoop.app.ui.screen.settings.SettingsVideoAudioScreen
import com.scoop.app.ui.theme.Motion

private val enterFromEnd: AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> EnterTransition = {
    slideInHorizontally(animationSpec = tween(Motion.EMPHASIZED_MS, easing = Motion.EmphasizedDecelerate), initialOffsetX = { it / 3 }) +
        fadeIn(animationSpec = tween(Motion.EMPHASIZED_MS, easing = Motion.EmphasizedDecelerate))
}

private val exitToStart: AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> ExitTransition = {
    slideOutHorizontally(animationSpec = tween(Motion.STANDARD_MS, easing = Motion.EmphasizedAccelerate), targetOffsetX = { -it / 5 }) +
        fadeOut(animationSpec = tween(Motion.QUICK_MS, easing = Motion.EmphasizedAccelerate))
}

private val enterFromStart: AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> EnterTransition = {
    slideInHorizontally(animationSpec = tween(Motion.EMPHASIZED_MS, easing = Motion.EmphasizedDecelerate), initialOffsetX = { -it / 3 }) +
        fadeIn(animationSpec = tween(Motion.EMPHASIZED_MS, easing = Motion.EmphasizedDecelerate))
}

private val exitToEnd: AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> ExitTransition = {
    slideOutHorizontally(animationSpec = tween(Motion.STANDARD_MS, easing = Motion.EmphasizedAccelerate), targetOffsetX = { it / 5 }) +
        fadeOut(animationSpec = tween(Motion.QUICK_MS, easing = Motion.EmphasizedAccelerate))
}

// Home <-> Settings hub is a container transform (the settings icon morphs into the screen via
// SharedTransitionLayout below), so those two destinations fade instead of sliding - a
// simultaneous slide would fight the bounds animation. Home -> Downloads keeps the normal slide.
private val homeExit: AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> ExitTransition = {
    if (targetState.destination.route == Route.SETTINGS_HUB) {
        fadeOut(animationSpec = tween(Motion.QUICK_MS))
    } else {
        exitToStart()
    }
}

private val homePopEnter: AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> EnterTransition = {
    if (initialState.destination.route == Route.SETTINGS_HUB) {
        fadeIn(animationSpec = tween(Motion.CONTAINER_TRANSFORM_MS))
    } else {
        enterFromStart()
    }
}

private val settingsHubEnter: AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(animationSpec = tween(Motion.CONTAINER_TRANSFORM_MS))
}

private val settingsHubPopExit: AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(animationSpec = tween(Motion.QUICK_MS))
}

/**
 * Home is the single root/hub (matching the reference app's model): Settings and Downloads are
 * reached via icons on Home and pushed as normal back-stack destinations, not bottom-nav tabs.
 * Home <-> Settings uses a shared-element container transform (the settings icon morphs into the
 * Settings screen), so the whole graph lives inside one SharedTransitionLayout.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ScoopNavHost(startUrl: String? = null) {
    val navController = rememberNavController()

    SharedTransitionLayout {
        val sharedTransitionScope = this

        NavHost(navController = navController, startDestination = Route.HOME) {
            composable(Route.HOME, exitTransition = homeExit, popEnterTransition = homePopEnter) {
                HomeScreen(
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = this,
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

            composable(Route.SETTINGS_HUB, enterTransition = settingsHubEnter, exitTransition = exitToStart, popEnterTransition = enterFromStart, popExitTransition = settingsHubPopExit) {
                SettingsHubScreen(
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = this,
                    onBack = { navController.popBackStack() },
                    onOpenGeneral = { navController.navigate(Route.SETTINGS_GENERAL) },
                    onOpenDownloads = { navController.navigate(Route.SETTINGS_DOWNLOADS) },
                    onOpenVideoAudio = { navController.navigate(Route.SETTINGS_VIDEO_AUDIO) },
                    onOpenStorage = { navController.navigate(Route.SETTINGS_STORAGE) },
                    onOpenAbout = { navController.navigate(Route.SETTINGS_ABOUT) },
                )
            }
            composable(Route.SETTINGS_GENERAL, enterTransition = enterFromEnd, exitTransition = exitToStart, popEnterTransition = enterFromStart, popExitTransition = exitToEnd) {
                SettingsGeneralScreen(onBack = { navController.popBackStack() })
            }
            composable(Route.SETTINGS_DOWNLOADS, enterTransition = enterFromEnd, exitTransition = exitToStart, popEnterTransition = enterFromStart, popExitTransition = exitToEnd) {
                SettingsDownloadsScreen(onBack = { navController.popBackStack() })
            }
            composable(Route.SETTINGS_VIDEO_AUDIO, enterTransition = enterFromEnd, exitTransition = exitToStart, popEnterTransition = enterFromStart, popExitTransition = exitToEnd) {
                SettingsVideoAudioScreen(onBack = { navController.popBackStack() })
            }
            composable(Route.SETTINGS_STORAGE, enterTransition = enterFromEnd, exitTransition = exitToStart, popEnterTransition = enterFromStart, popExitTransition = exitToEnd) {
                SettingsStorageScreen(onBack = { navController.popBackStack() })
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
}
