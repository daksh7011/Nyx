package com.slothiesmooth.nyx.feature.settings.basic

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.slothiesmooth.nyx.feature.common.api.FeatureContext
import com.slothiesmooth.nyx.feature.common.koin.KoinFeatureProvider
import com.slothiesmooth.nyx.feature.settings.api.SettingsFeature
import com.slothiesmooth.nyx.feature.settings.api.SettingsLicensesRoute
import com.slothiesmooth.nyx.feature.settings.api.SettingsRoute
import com.slothiesmooth.nyx.feature.settings.basic.domain.usecase.WipeVaultUseCase
import com.slothiesmooth.nyx.feature.settings.basic.presentation.SettingsScreen
import com.slothiesmooth.nyx.feature.settings.basic.presentation.SettingsViewModel
import com.slothiesmooth.nyx.feature.settings.basic.presentation.licenses.LicensesScreen
import com.slothiesmooth.nyx.feature.theme.api.ThemeRoute
import com.slothiesmooth.nyx.shared.data.event.DomainEventBus
import com.slothiesmooth.nyx.shared.data.source.AppInfo
import com.slothiesmooth.nyx.shared.data.source.VaultFileStore
import com.slothiesmooth.nyx.shared.data.source.VaultSource
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf

/**
 * The settings feature: about label, appearance (routes out to [ThemeRoute]), OSS licenses, source
 * link, and the danger-zone vault wipe. Wires the outer sources/bus/[appInfo] it receives into an
 * isolated Koin graph that builds this feature's use case and view model.
 */
class BasicSettingsProvider(
    private val vaultSource: VaultSource,
    private val fileStore: VaultFileStore,
    private val eventBus: DomainEventBus,
    private val appInfo: AppInfo,
) : KoinFeatureProvider(), SettingsFeature {

    @Composable
    override fun onProvideContent(context: FeatureContext, content: @Composable () -> Unit) = content()

    override fun onProvideNavigation(context: FeatureContext, builder: NavGraphBuilder) {
        builder.composable<SettingsRoute> {
            withDI {
                SettingsScreen(
                    onOpenTheme = { context.pushDestination(ThemeRoute) },
                    onOpenLicenses = { context.pushDestination(SettingsLicensesRoute) },
                )
            }
        }
        builder.composable<SettingsLicensesRoute> {
            withDI { LicensesScreen(onBack = { context.popDestination() }) }
        }
    }

    override fun Module.onProvideDI() {
        single { vaultSource }
        single { fileStore }
        single { eventBus }
        single { appInfo }
        factoryOf(::WipeVaultUseCase)
        viewModelOf(::SettingsViewModel)
    }
}
