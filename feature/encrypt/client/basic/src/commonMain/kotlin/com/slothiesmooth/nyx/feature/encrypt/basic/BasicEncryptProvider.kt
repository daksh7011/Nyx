package com.slothiesmooth.nyx.feature.encrypt.basic

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.slothiesmooth.nyx.designlibrary.molecules.NxEmptyState
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.feature.common.api.BaseFeatureProvider
import com.slothiesmooth.nyx.feature.common.api.FeatureContext
import com.slothiesmooth.nyx.feature.encrypt.api.EncryptFeature
import com.slothiesmooth.nyx.feature.encrypt.api.EncryptRoute

/** Placeholder encrypt provider; Plan 06 replaces the screen with the real encrypt wizard. */
class BasicEncryptProvider : BaseFeatureProvider(), EncryptFeature {

    @Composable
    override fun onProvideContent(context: FeatureContext, content: @Composable () -> Unit) = content()

    override fun onProvideNavigation(context: FeatureContext, builder: NavGraphBuilder) {
        builder.composable<EncryptRoute> {
            NxEmptyState(
                icon = NxIconKind.Lock,
                title = "Encrypt",
                body = "Hide an encrypted message inside an image. Coming soon.",
            )
        }
    }
}
