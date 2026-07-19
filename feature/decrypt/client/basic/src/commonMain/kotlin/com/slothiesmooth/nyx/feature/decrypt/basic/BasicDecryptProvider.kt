package com.slothiesmooth.nyx.feature.decrypt.basic

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.slothiesmooth.nyx.crypto.NyxCrypto
import com.slothiesmooth.nyx.feature.common.api.FeatureContext
import com.slothiesmooth.nyx.feature.common.koin.KoinFeatureProvider
import com.slothiesmooth.nyx.feature.decrypt.api.DecryptFeature
import com.slothiesmooth.nyx.feature.decrypt.api.DecryptRoute
import com.slothiesmooth.nyx.feature.decrypt.basic.domain.usecase.DecryptMessageUseCase
import com.slothiesmooth.nyx.feature.decrypt.basic.domain.usecase.LoadVaultImageBytesUseCase
import com.slothiesmooth.nyx.feature.decrypt.basic.presentation.DecryptScreen
import com.slothiesmooth.nyx.feature.decrypt.basic.presentation.DecryptViewModel
import com.slothiesmooth.nyx.shared.data.source.ClipboardWriter
import com.slothiesmooth.nyx.shared.data.source.ImageCodec
import com.slothiesmooth.nyx.shared.data.source.VaultFileStore
import com.slothiesmooth.nyx.steganography.Steganography
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf

/**
 * The decrypt feature: reveals a message hidden inside an image, either picked fresh or loaded
 * from the vault by [DecryptRoute.imageId]. Wires the outer engines/sources it receives into an
 * isolated Koin graph that builds the reveal use cases and view model.
 */
class BasicDecryptProvider(
    private val crypto: NyxCrypto,
    private val stego: Steganography,
    private val codec: ImageCodec,
    private val fileStore: VaultFileStore,
    private val clipboardWriter: ClipboardWriter,
) : KoinFeatureProvider(), DecryptFeature {

    @Composable
    override fun onProvideContent(context: FeatureContext, content: @Composable () -> Unit) = content()

    override fun onProvideNavigation(context: FeatureContext, builder: NavGraphBuilder) {
        builder.composable<DecryptRoute> { entry ->
            val route = entry.toRoute<DecryptRoute>()
            withDI {
                DecryptScreen(imageId = route.imageId, onBack = { context.popDestination() })
            }
        }
    }

    override fun Module.onProvideDI() {
        single { crypto }
        single { stego }
        single { codec }
        single { fileStore }
        single { clipboardWriter }
        factoryOf(::DecryptMessageUseCase)
        factoryOf(::LoadVaultImageBytesUseCase)
        viewModelOf(::DecryptViewModel)
    }
}
