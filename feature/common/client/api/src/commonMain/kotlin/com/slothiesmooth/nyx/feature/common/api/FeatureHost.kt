package com.slothiesmooth.nyx.feature.common.api

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.slothiesmooth.nyx.shared.presentation.navigation.popDestination
import com.slothiesmooth.nyx.shared.presentation.navigation.pushDestination
import com.slothiesmooth.nyx.shared.presentation.navigation.restoreDestination
import com.slothiesmooth.nyx.shared.presentation.navigation.setDestination
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val nextFeatureIndex = staticCompositionLocalOf { 0 }

/**
 * The bottom-bar slot for the single shell Scaffold. A feature (the navigation feature) publishes a
 * composable here from inside its [FeatureProvider.provideContent] wrapper; the terminal host reads it.
 * Default is empty, so a shell with no navigation feature (e.g. a splash-only test) has no bottom bar.
 */
val LocalFeatureBottomBar = staticCompositionLocalOf<@Composable () -> Unit> { {} }

/**
 * Recursively nests each registered [FeatureProvider]'s content wrapper (index advanced through
 * [nextFeatureIndex]); once every feature has wrapped, the terminal [FeatureHostContent] draws the
 * one Scaffold + NavHost from all features' `provideNavigation` contributions.
 */
@Composable
fun FeatureHost(context: FeatureHostContext, startDestinationProvider: () -> Any?) {
    val index = nextFeatureIndex.current
    val feature = remember(index) { context.features.getOrNull(index) as? FeatureProvider }
    if (feature != null) {
        CompositionLocalProvider(nextFeatureIndex provides index + 1) {
            feature.provideContent(context) {
                FeatureHost(context, startDestinationProvider)
            }
        }
    } else {
        FeatureHostContent(context, startDestinationProvider)
    }
}

@Composable
private fun FeatureHostContent(context: FeatureHostContext, startDestinationProvider: () -> Any?) {
    val startDestination = startDestinationProvider() ?: return
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = LocalFeatureBottomBar.current,
    ) { paddings ->
        NavHost(
            modifier = Modifier.fillMaxSize().padding(paddings).consumeWindowInsets(paddings),
            startDestination = startDestination,
            contentAlignment = Alignment.Center,
            navController = context.navController,
        ) {
            context.features.forEach { registered ->
                (registered as? FeatureProvider)?.provideNavigation(context, this)
            }
        }
    }
}

/**
 * The concrete [FeatureContext]. `replaceDestination` is inlined (not a shared nav verb) because its
 * `popUpTo(route: Any)` was ambiguous on wasmJs; `currentDestination?.route` is a String, so
 * `popUpTo(String)` is safe.
 */
data class FeatureHostContext(
    internal val debug: Boolean,
    internal val features: List<Feature>,
    internal val navController: NavHostController,
) : FeatureContext {

    override fun getCurrentDestinationChanges(): Flow<String?> = navController.currentBackStackEntryFlow
        .map { entry -> entry.destination.route }
        .distinctUntilChanged()

    override fun getCurrentDestination(): String? = navController.currentBackStackEntry?.destination?.route

    override fun replaceDestination(route: Any) {
        navController.navigate(route) {
            navController.currentDestination?.route?.let { current -> popUpTo(current) { inclusive = true } }
            launchSingleTop = true
        }
    }

    override fun restoreDestination(route: Any) = navController.restoreDestination(route)

    override fun setDestination(route: Any) = navController.setDestination(route)

    override fun pushDestination(route: Any) = navController.pushDestination(route)

    override fun popDestination() = navController.popDestination()
}
