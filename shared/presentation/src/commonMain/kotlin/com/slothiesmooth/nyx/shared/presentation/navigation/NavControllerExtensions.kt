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

/** Clears back to the graph start (exclusive) and navigates to [route] — used for tab switches. */
fun NavController.setDestination(route: Any) {
    navigate(
        route,
        navOptions {
            graph.startDestinationRoute?.let { graphRoute ->
                popUpTo(graphRoute) { inclusive = false }
            }
        },
    )
}
