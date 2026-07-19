package com.slothiesmooth.nyx.client.app

import com.slothiesmooth.nyx.client.app.presentation.AppViewModel
import com.slothiesmooth.nyx.client.data.codec.defaultImageCodec
import com.slothiesmooth.nyx.crypto.DefaultNyxCrypto
import com.slothiesmooth.nyx.crypto.NyxCrypto
import com.slothiesmooth.nyx.feature.common.api.Feature
import com.slothiesmooth.nyx.feature.decrypt.api.DecryptFeature
import com.slothiesmooth.nyx.feature.decrypt.basic.BasicDecryptProvider
import com.slothiesmooth.nyx.feature.encrypt.api.EncryptFeature
import com.slothiesmooth.nyx.feature.encrypt.basic.BasicEncryptProvider
import com.slothiesmooth.nyx.feature.encrypt.basic.EncryptFeatureDependencies
import com.slothiesmooth.nyx.feature.navigation.api.NavigationFeature
import com.slothiesmooth.nyx.feature.navigation.basic.BasicNavigationProvider
import com.slothiesmooth.nyx.feature.settings.api.SettingsFeature
import com.slothiesmooth.nyx.feature.settings.basic.BasicSettingsProvider
import com.slothiesmooth.nyx.feature.splash.api.SplashFeature
import com.slothiesmooth.nyx.feature.splash.basic.BasicSplashProvider
import com.slothiesmooth.nyx.feature.theme.api.ThemeFeature
import com.slothiesmooth.nyx.feature.theme.basic.BasicThemeProvider
import com.slothiesmooth.nyx.feature.theme.basic.ThemeRepository
import com.slothiesmooth.nyx.feature.vault.api.VaultFeature
import com.slothiesmooth.nyx.feature.vault.api.VaultRoute
import com.slothiesmooth.nyx.feature.vault.basic.BasicVaultProvider
import com.slothiesmooth.nyx.shared.data.event.DefaultDomainEventBus
import com.slothiesmooth.nyx.shared.data.event.DomainEventBus
import com.slothiesmooth.nyx.shared.data.id.IdGenerator
import com.slothiesmooth.nyx.shared.data.id.Uuid4IdGenerator
import com.slothiesmooth.nyx.shared.data.source.ImageCodec
import com.slothiesmooth.nyx.shared.data.source.VaultSource
import com.slothiesmooth.nyx.shared.data.time.Clock
import com.slothiesmooth.nyx.shared.data.time.SystemClock
import com.slothiesmooth.nyx.steganography.Steganography
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * The application graph. Layers the [platformModule] (drivers, settings, file/share/camera sources,
 * capabilities) under the shared engines/infra, one binding per feature interface, and the ordered
 * feature list the [com.slothiesmooth.nyx.feature.common.api.FeatureHost] decorates. Navigation is a
 * one-off through each provider, so splash gets its cross-feature target as an `Any` route.
 */
fun appModule(platformModule: Module): Module = module {
    includes(platformModule)

    // Engines (Plan 02) and infrastructure (Plan 03).
    single<NyxCrypto> { DefaultNyxCrypto() }
    single { Steganography() }
    single<ImageCodec> { defaultImageCodec() }
    single<IdGenerator> { Uuid4IdGenerator() }
    single<Clock> { SystemClock() }
    single<DomainEventBus> { DefaultDomainEventBus() }
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    // SqlDelight-backed vault metadata source (schema + wrappers delivered by Plan 03).
    single { com.slothiesmooth.nyx.client.data.source.database.sqldelight.SqlDelightSource(get(), get()) }
    single<VaultSource> { com.slothiesmooth.nyx.client.data.source.database.vault.VaultSqlSource(get()) }

    registerFeatures()

    // The nested order: splash + navigation wrap first, then the tab features contribute routes.
    single<List<Feature>> {
        listOf(
            get<SplashFeature>(),
            get<NavigationFeature>(),
            get<ThemeFeature>(),
            get<VaultFeature>(),
            get<EncryptFeature>(),
            get<DecryptFeature>(),
            get<SettingsFeature>(),
        )
    }

    viewModelOf(::AppViewModel)
}

/** Binds each feature to its cross-feature interface within the app graph. */
private fun Module.registerFeatures() {
    single<ThemeFeature> { BasicThemeProvider(ThemeRepository(get()), get()) }
    single<NavigationFeature> { BasicNavigationProvider() }
    single<SplashFeature> { BasicSplashProvider(afterSplashRoute = VaultRoute) }
    single<VaultFeature> {
        BasicVaultProvider(
            vaultSource = get(),
            fileStore = get(),
            clock = get(),
            eventBus = get(),
            shareSource = get(),
        )
    }
    single<EncryptFeature> {
        BasicEncryptProvider(
            EncryptFeatureDependencies(
                crypto = get(),
                stego = get(),
                codec = get(),
                imagePicker = get(),
                vaultSource = get(),
                fileStore = get(),
                idGenerator = get(),
                clock = get(),
                eventBus = get(),
                cameraSource = get(),
                shareSource = get(),
                capabilities = get(),
            ),
        )
    }
    single<DecryptFeature> {
        BasicDecryptProvider(
            crypto = get(),
            stego = get(),
            codec = get(),
            fileStore = get(),
            imagePicker = get(),
            clipboardWriter = get(),
        )
    }
    single<SettingsFeature> {
        BasicSettingsProvider(vaultSource = get(), fileStore = get(), eventBus = get(), appInfo = get())
    }
}
