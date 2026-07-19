package com.slothiesmooth.nyx.feature.encrypt.basic.domain.usecase

import com.slothiesmooth.nyx.shared.data.event.DomainEvent
import com.slothiesmooth.nyx.shared.data.event.DomainEventBus
import com.slothiesmooth.nyx.shared.data.id.IdGenerator
import com.slothiesmooth.nyx.shared.data.id.StegoImageId
import com.slothiesmooth.nyx.shared.data.result.AppError
import com.slothiesmooth.nyx.shared.data.result.AppResult
import com.slothiesmooth.nyx.shared.data.source.StegoImageRecord
import com.slothiesmooth.nyx.shared.data.source.VaultFileStore
import com.slothiesmooth.nyx.shared.data.source.VaultSource
import com.slothiesmooth.nyx.shared.data.time.Clock
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

private const val ID_PREFIX_LENGTH = 8
private const val SAVE_FAILED = "Failed to save image to the vault"

/**
 * Writes stego PNG bytes to the vault: file first, then metadata row. A blank [invoke] `name` is
 * replaced with an auto-generated `nyx-<id-prefix>.png`. If the metadata write fails, the file
 * write is rolled back so the vault never holds an orphaned file.
 */
class SaveToVaultUseCase(
    private val vaultSource: VaultSource,
    private val fileStore: VaultFileStore,
    private val idGenerator: IdGenerator,
    private val clock: Clock,
    private val eventBus: DomainEventBus,
) {
    suspend operator fun invoke(pngBytes: ByteArray, name: String): AppResult<StegoImageId> {
        val id = idGenerator.newId()
        when (val write = fileStore.write(id, pngBytes)) {
            is AppResult.Ok -> Unit
            is AppResult.Err -> return write
        }
        val result = recordMetadata(id, name)
        if (result is AppResult.Err) fileStore.delete(id)
        return result
    }

    private suspend fun recordMetadata(id: String, name: String): AppResult<StegoImageId> {
        val resolvedName = name.ifBlank { "nyx-${id.take(ID_PREFIX_LENGTH)}.png" }
        val now = clock.now().toString()
        val record = StegoImageRecord(
            id = id,
            name = resolvedName,
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
            isArchived = false,
        )
        // runCatching also traps CancellationException; rethrow it so structured cancellation works.
        val upsert = runCatching { vaultSource.upsert(record) }
        coroutineContext.ensureActive()
        if (upsert.isFailure) {
            return AppResult.Err(AppError.Storage(SAVE_FAILED, upsert.exceptionOrNull()))
        }
        val imageId = StegoImageId(id)
        eventBus.emit(DomainEvent.StegoImageStored(imageId))
        return AppResult.Ok(imageId)
    }
}
