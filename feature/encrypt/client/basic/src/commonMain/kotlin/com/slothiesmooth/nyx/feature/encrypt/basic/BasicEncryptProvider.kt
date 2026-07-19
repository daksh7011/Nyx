package com.slothiesmooth.nyx.feature.encrypt.basic

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.slothiesmooth.nyx.feature.common.api.FeatureContext
import com.slothiesmooth.nyx.feature.common.koin.KoinFeatureProvider
import com.slothiesmooth.nyx.feature.encrypt.api.EncryptFeature
import com.slothiesmooth.nyx.feature.encrypt.api.EncryptRoute
import com.slothiesmooth.nyx.feature.encrypt.basic.domain.usecase.EncryptMessageUseCase
import com.slothiesmooth.nyx.feature.encrypt.basic.domain.usecase.SaveToVaultUseCase
import com.slothiesmooth.nyx.feature.encrypt.basic.presentation.EncryptScreen
import com.slothiesmooth.nyx.feature.encrypt.basic.presentation.EncryptViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf

/**
 * The encrypt feature: hides an encrypted message inside a cover image and, when the platform
 * supports a persistent vault, saves the result. Wires the outer engines/sources it receives
 * (bundled in [dependencies]) into an isolated Koin graph that builds this wizard's use cases and
 * view model.
 */
class BasicEncryptProvider(
    private val dependencies: EncryptFeatureDependencies,
) : KoinFeatureProvider(), EncryptFeature {

    @Composable
    override fun onProvideContent(context: FeatureContext, content: @Composable () -> Unit) = content()

    override fun onProvideNavigation(context: FeatureContext, builder: NavGraphBuilder) {
        builder.composable<EncryptRoute> {
            withDI { EncryptScreen(onClose = { context.popDestination() }) }
        }
    }

    override fun Module.onProvideDI() {
        single { dependencies.crypto }
        single { dependencies.stego }
        single { dependencies.codec }
        single { dependencies.vaultSource }
        single { dependencies.fileStore }
        single { dependencies.idGenerator }
        single { dependencies.clock }
        single { dependencies.eventBus }
        single { dependencies.cameraSource }
        single { dependencies.shareSource }
        single { dependencies.capabilities }
        factoryOf(::EncryptMessageUseCase)
        factoryOf(::SaveToVaultUseCase)
        viewModelOf(::EncryptViewModel)
    }
}
