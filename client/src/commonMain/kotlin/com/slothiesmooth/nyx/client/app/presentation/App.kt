package com.slothiesmooth.nyx.client.app.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.slothiesmooth.nyx.feature.common.api.Feature
import com.slothiesmooth.nyx.feature.common.api.FeatureHost
import com.slothiesmooth.nyx.feature.common.api.FeatureHostContext
import com.slothiesmooth.nyx.feature.theme.api.ThemeFeature
import com.slothiesmooth.nyx.feature.theme.basic.ThemeProvider
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Root composable. Applies the persisted theme via [ThemeProvider], builds the single-NavHost
 * [FeatureHost] over the ordered feature list, and keeps the bottom-nav selection in sync with the
 * live back-stack entry. The Koin graph is established by [com.slothiesmooth.nyx.client.initKoin].
 */
@Composable
fun App() {
    val themeFeature = koinInject<ThemeFeature>()
    val features = koinInject<List<Feature>>()
    val appViewModel = koinViewModel<AppViewModel>()
    appViewModel.bind()

    val navController = rememberNavController()
    val context = remember(navController, features) {
        FeatureHostContext(debug = false, features = features, navController = navController)
    }

    val entry by navController.currentBackStackEntryAsState()
    LaunchedEffect(entry) { appViewModel.refreshNavItems(entry?.destination?.route) }

    ThemeProvider(themeFeature) {
        FeatureHost(context = context, startDestinationProvider = { appViewModel.startDestination })
    }
}
