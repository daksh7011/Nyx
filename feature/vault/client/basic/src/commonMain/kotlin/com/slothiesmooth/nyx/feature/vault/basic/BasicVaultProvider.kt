package com.slothiesmooth.nyx.feature.vault.basic

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.slothiesmooth.nyx.feature.common.api.FeatureContext
import com.slothiesmooth.nyx.feature.common.koin.KoinFeatureProvider
import com.slothiesmooth.nyx.feature.decrypt.api.DecryptRoute
import com.slothiesmooth.nyx.feature.encrypt.api.EncryptRoute
import com.slothiesmooth.nyx.feature.vault.api.VaultDetailRoute
import com.slothiesmooth.nyx.feature.vault.api.VaultFeature
import com.slothiesmooth.nyx.feature.vault.api.VaultRoute
import com.slothiesmooth.nyx.feature.vault.basic.data.VaultRepositoryImpl
import com.slothiesmooth.nyx.feature.vault.basic.domain.VaultRepository
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.ArchiveImageUseCase
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.DeleteImageUseCase
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.GetImageBytesUseCase
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.ObserveArchivedImagesUseCase
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.ObserveVaultImagesUseCase
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.RestoreImageUseCase
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.ShareVaultImageUseCase
import com.slothiesmooth.nyx.feature.vault.basic.presentation.VaultImageUseCases
import com.slothiesmooth.nyx.feature.vault.basic.presentation.detail.VaultDetailScreen
import com.slothiesmooth.nyx.feature.vault.basic.presentation.detail.VaultDetailViewModel
import com.slothiesmooth.nyx.feature.vault.basic.presentation.list.VaultScreen
import com.slothiesmooth.nyx.feature.vault.basic.presentation.list.VaultViewModel
import com.slothiesmooth.nyx.shared.data.event.DomainEventBus
import com.slothiesmooth.nyx.shared.data.source.ShareSource
import com.slothiesmooth.nyx.shared.data.source.VaultFileStore
import com.slothiesmooth.nyx.shared.data.source.VaultSource
import com.slothiesmooth.nyx.shared.data.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf

/**
 * The vault feature: hosts the stego-image grid + detail routes over an isolated Koin graph built
 * from the outer sources/clock/bus it receives. [observeActiveCount] is the cross-feature signal
 * (e.g. driving an empty-state gate elsewhere), backed directly by [vaultSource] since it needs no
 * feature-local state.
 */
class BasicVaultProvider(
    private val vaultSource: VaultSource,
    private val fileStore: VaultFileStore,
    private val clock: Clock,
    private val eventBus: DomainEventBus,
    private val shareSource: ShareSource,
) : KoinFeatureProvider(), VaultFeature {

    override fun observeActiveCount(): Flow<Int> = vaultSource.observeActive().map { it.size }

    @Composable
    override fun onProvideContent(context: FeatureContext, content: @Composable () -> Unit) = content()

    override fun onProvideNavigation(context: FeatureContext, builder: NavGraphBuilder) {
        builder.composable<VaultRoute> {
            withDI {
                VaultScreen(
                    onOpenDetail = { id -> context.pushDestination(VaultDetailRoute(id.value)) },
                    onOpenEncrypt = { context.pushDestination(EncryptRoute) },
                )
            }
        }
        builder.composable<VaultDetailRoute> { entry ->
            val route = entry.toRoute<VaultDetailRoute>()
            withDI {
                VaultDetailScreen(
                    imageId = route.imageId,
                    onBack = { context.popDestination() },
                    onDecryptThis = { imageId -> context.pushDestination(DecryptRoute(imageId)) },
                )
            }
        }
    }

    override fun Module.onProvideDI() {
        single { vaultSource }
        single { fileStore }
        single { clock }
        single { eventBus }
        single { shareSource }
        single<VaultRepository> { VaultRepositoryImpl(get(), get(), get(), get()) }
        factoryOf(::ObserveVaultImagesUseCase)
        factoryOf(::ObserveArchivedImagesUseCase)
        factoryOf(::GetImageBytesUseCase)
        factoryOf(::ArchiveImageUseCase)
        factoryOf(::RestoreImageUseCase)
        factoryOf(::DeleteImageUseCase)
        factoryOf(::ShareVaultImageUseCase)
        factoryOf(::VaultImageUseCases)
        viewModelOf(::VaultViewModel)
        viewModelOf(::VaultDetailViewModel)
    }
}
