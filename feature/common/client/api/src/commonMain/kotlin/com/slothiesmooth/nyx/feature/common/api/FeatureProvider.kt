package com.slothiesmooth.nyx.feature.common.api

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder

interface FeatureProvider : Feature {

    /** Wraps the recursive host content; the outermost providers get to decorate the whole shell. */
    @Composable
    fun provideContent(context: FeatureContext, content: @Composable () -> Unit)

    /** Contributes this feature's type-safe routes to the single shell NavHost. */
    fun provideNavigation(context: FeatureContext, builder: NavGraphBuilder)
}
