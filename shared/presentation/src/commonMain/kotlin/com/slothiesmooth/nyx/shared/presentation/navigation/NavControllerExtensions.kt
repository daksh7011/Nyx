package com.slothiesmooth.nyx.shared.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.navOptions

/** Pops the back stack if there is a previous entry. */
fun NavController.popDestination() {
    if (previousBackStackEntry != null) {
        popBackStack()
    }
}

/** Navigates to a new instance of [route]. */
fun NavController.pushDestination(route: Any) {
    navigate(route)
}

/**
 * Navigates to [route] as a single top instance, popping any existing instance of it first.
 */
fun NavController.restoreDestination(route: Any) {
    navigate(
        route,
        navOptions {
            popUpTo(route) { inclusive = true }
            launchSingleTop = true
            restoreState = false
        },
    )
}

/**
 * Clears back to the back-stack root (exclusive) and navigates to [route] — used for tab switches, so
 * the tabs stay single-level and Back from the root tab exits the app. Anchors on the current root
 * rather than `graph.startDestinationRoute`, because the transient splash is the graph's declared
 * start yet is popped once it advances (see BasicSplashProvider).
 */
fun NavController.setDestination(route: Any) {
    val rootRoute = currentBackStack.value.firstOrNull { entry -> entry.destination.route != null }
        ?.destination
        ?.route
    navigate(
        route,
        navOptions {
            rootRoute?.let { home -> popUpTo(home) { inclusive = false } }
            launchSingleTop = true
        },
    )
}
