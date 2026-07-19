package com.slothiesmooth.nyx.feature.vault.basic

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.slothiesmooth.nyx.designlibrary.molecules.NxEmptyState
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.feature.common.api.BaseFeatureProvider
import com.slothiesmooth.nyx.feature.common.api.FeatureContext
import com.slothiesmooth.nyx.feature.vault.api.VaultFeature
import com.slothiesmooth.nyx.feature.vault.api.VaultRoute
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Placeholder vault provider; Plan 06 replaces the screen with the real stego-image grid. */
class BasicVaultProvider : BaseFeatureProvider(), VaultFeature {

    override fun observeActiveCount(): Flow<Int> = flowOf(0)

    @Composable
    override fun onProvideContent(context: FeatureContext, content: @Composable () -> Unit) = content()

    override fun onProvideNavigation(context: FeatureContext, builder: NavGraphBuilder) {
        builder.composable<VaultRoute> {
            NxEmptyState(
                icon = NxIconKind.Vault,
                title = "Vault",
                body = "Your saved hidden messages will live here. Coming soon.",
            )
        }
    }
}
