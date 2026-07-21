package com.slothiesmooth.nyx.feature.splash.basic

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.slothiesmooth.nyx.feature.common.api.BaseFeatureProvider
import com.slothiesmooth.nyx.feature.common.api.FeatureContext
import com.slothiesmooth.nyx.feature.common.koin.KoinFeatureProvider
import com.slothiesmooth.nyx.feature.common.koin.koinFeatureViewModel
import com.slothiesmooth.nyx.feature.splash.api.SplashFeature
import com.slothiesmooth.nyx.feature.splash.api.SplashRoute
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf

/** Hosts [SplashRoute] and, when its dwell elapses, replaces it with the injected start destination. */
class BasicSplashProvider(
    private val afterSplashRoute: Any,
) : KoinFeatureProvider(), SplashFeature {

    @Composable
    override fun onProvideContent(context: FeatureContext, content: @Composable () -> Unit) {
        withDI { content() }
    }

    override fun onProvideNavigation(context: FeatureContext, builder: NavGraphBuilder) {
        builder.composable<SplashRoute> {
            withDI {
                SplashScreen(viewModel = koinFeatureViewModel(), onReady = { onSendAction(Advance) })
            }
        }
    }

    override suspend fun onReceiveAction(action: BaseFeatureProvider.Action, context: FeatureContext) {
        // replaceDestination pops splash off the back stack, so it becomes the app root: Back on the
        // start screen exits the app instead of flashing splash, and rapid Back can't re-enter it.
        if (action is Advance) context.replaceDestination(afterSplashRoute)
    }

    override fun Module.onProvideDI() {
        factoryOf(::SplashMutableState)
        viewModelOf(::SplashViewModel)
    }

    private data object Advance : BaseFeatureProvider.Action
}
