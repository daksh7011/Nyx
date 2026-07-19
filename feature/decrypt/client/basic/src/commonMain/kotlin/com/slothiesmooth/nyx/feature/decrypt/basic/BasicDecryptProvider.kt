package com.slothiesmooth.nyx.feature.decrypt.basic

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.slothiesmooth.nyx.designlibrary.molecules.NxEmptyState
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.feature.common.api.BaseFeatureProvider
import com.slothiesmooth.nyx.feature.common.api.FeatureContext
import com.slothiesmooth.nyx.feature.decrypt.api.DecryptFeature
import com.slothiesmooth.nyx.feature.decrypt.api.DecryptRoute

/** Placeholder decrypt provider; Plan 06 replaces the screen with the real reveal flow. */
class BasicDecryptProvider : BaseFeatureProvider(), DecryptFeature {

    @Composable
    override fun onProvideContent(context: FeatureContext, content: @Composable () -> Unit) = content()

    override fun onProvideNavigation(context: FeatureContext, builder: NavGraphBuilder) {
        builder.composable<DecryptRoute> {
            NxEmptyState(
                icon = NxIconKind.Unlock,
                title = "Decrypt",
                body = "Reveal a hidden message from an image. Coming soon.",
            )
        }
    }
}
