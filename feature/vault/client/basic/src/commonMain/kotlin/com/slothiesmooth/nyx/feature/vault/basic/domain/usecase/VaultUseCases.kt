package com.slothiesmooth.nyx.feature.vault.basic.domain.usecase

import com.slothiesmooth.nyx.feature.vault.basic.domain.VaultRepository
import com.slothiesmooth.nyx.feature.vault.basic.domain.model.VaultImage
import com.slothiesmooth.nyx.shared.data.id.StegoImageId
import com.slothiesmooth.nyx.shared.data.result.AppResult
import com.slothiesmooth.nyx.shared.data.source.ShareSource
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow

class ObserveVaultImagesUseCase(private val repository: VaultRepository) {
    operator fun invoke(): Flow<ImmutableList<VaultImage>> = repository.observeActive()
}

class ObserveArchivedImagesUseCase(private val repository: VaultRepository) {
    operator fun invoke(): Flow<ImmutableList<VaultImage>> = repository.observeArchived()
}

class GetImageBytesUseCase(private val repository: VaultRepository) {
    suspend operator fun invoke(id: StegoImageId): AppResult<ByteArray> = repository.imageBytes(id)
}

class ArchiveImageUseCase(private val repository: VaultRepository) {
    suspend operator fun invoke(id: StegoImageId): AppResult<Unit> = repository.archive(id)
}

class RestoreImageUseCase(private val repository: VaultRepository) {
    suspend operator fun invoke(id: StegoImageId): AppResult<Unit> = repository.restore(id)
}

class DeleteImageUseCase(private val repository: VaultRepository) {
    suspend operator fun invoke(id: StegoImageId): AppResult<Unit> = repository.softDelete(id)
}

class ShareVaultImageUseCase(
    private val repository: VaultRepository,
    private val shareSource: ShareSource,
) {
    suspend operator fun invoke(id: StegoImageId, fileName: String): AppResult<Unit> =
        when (val bytes = repository.imageBytes(id)) {
            is AppResult.Ok -> shareSource.shareImage(bytes.value, fileName)
            is AppResult.Err -> bytes
        }
}
