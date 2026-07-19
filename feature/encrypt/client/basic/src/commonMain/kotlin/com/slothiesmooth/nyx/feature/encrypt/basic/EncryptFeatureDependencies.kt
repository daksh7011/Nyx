package com.slothiesmooth.nyx.feature.encrypt.basic

import com.slothiesmooth.nyx.crypto.NyxCrypto
import com.slothiesmooth.nyx.shared.data.event.DomainEventBus
import com.slothiesmooth.nyx.shared.data.id.IdGenerator
import com.slothiesmooth.nyx.shared.data.source.CameraSource
import com.slothiesmooth.nyx.shared.data.source.ImageCodec
import com.slothiesmooth.nyx.shared.data.source.PlatformCapabilities
import com.slothiesmooth.nyx.shared.data.source.ShareSource
import com.slothiesmooth.nyx.shared.data.source.VaultFileStore
import com.slothiesmooth.nyx.shared.data.source.VaultSource
import com.slothiesmooth.nyx.shared.data.time.Clock
import com.slothiesmooth.nyx.steganography.Steganography

/**
 * Parameter object bundling the outer engines/sources [BasicEncryptProvider] re-registers into its
 * isolated Koin graph. A parameter object is detekt's root-cause fix for the `LongParameterList`
 * gate: the provider needs all eleven of these from the app graph, which exceeds the constructor
 * threshold as individual parameters.
 */
data class EncryptFeatureDependencies(
    val crypto: NyxCrypto,
    val stego: Steganography,
    val codec: ImageCodec,
    val vaultSource: VaultSource,
    val fileStore: VaultFileStore,
    val idGenerator: IdGenerator,
    val clock: Clock,
    val eventBus: DomainEventBus,
    val cameraSource: CameraSource,
    val shareSource: ShareSource,
    val capabilities: PlatformCapabilities,
)
