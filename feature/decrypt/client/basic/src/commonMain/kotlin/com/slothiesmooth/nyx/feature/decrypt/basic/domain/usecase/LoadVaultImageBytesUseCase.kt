package com.slothiesmooth.nyx.feature.decrypt.basic.domain.usecase

import com.slothiesmooth.nyx.shared.data.id.StegoImageId
import com.slothiesmooth.nyx.shared.data.result.AppResult
import com.slothiesmooth.nyx.shared.data.source.VaultFileStore

/** Reads the stored PNG bytes for a vault image by [id], ready to hand to [DecryptMessageUseCase]. */
class LoadVaultImageBytesUseCase(private val fileStore: VaultFileStore) {
    suspend operator fun invoke(id: StegoImageId): AppResult<ByteArray> = fileStore.read(id.value)
}
