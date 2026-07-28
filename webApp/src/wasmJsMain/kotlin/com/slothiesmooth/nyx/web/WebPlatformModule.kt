package com.slothiesmooth.nyx.web

import com.slothiesmooth.nyx.shared.data.source.AppInfo
import com.slothiesmooth.nyx.shared.data.source.CameraSource
import com.slothiesmooth.nyx.shared.data.source.ClipboardWriter
import com.slothiesmooth.nyx.shared.data.source.ImagePicker
import com.slothiesmooth.nyx.shared.data.source.PlatformCapabilities
import com.slothiesmooth.nyx.shared.data.source.SettingsSource
import com.slothiesmooth.nyx.shared.data.source.ShareSource
import com.slothiesmooth.nyx.shared.data.source.VaultFileStore
import com.slothiesmooth.nyx.shared.data.source.VaultSource
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * The web (wasmJs) infrastructure the app graph layers under: a session-only in-memory vault (no
 * SqlDelight driver on wasm), localStorage settings, browser download as "share", and no camera —
 * `persistentVault = false` drives the UI to an export-only vault.
 */
fun webPlatformModule(): Module = module {
    single<VaultSource> { InMemoryVaultSource() }
    single<VaultFileStore> { InMemoryVaultFileStore() }
    single<SettingsSource> { LocalStorageSettingsSource() }
    single<ShareSource> { WebShareSource() }
    single<CameraSource> { NoCameraSource }
    single<ImagePicker> { WebImagePicker() }
    single<ClipboardWriter> { WebClipboardWriter() }
    single<AppInfo> { WebAppInfo() }
    single { PlatformCapabilities(camera = false, persistentVault = false) }
}
