package com.slothiesmooth.nyx.feature.vault.basic.data

import com.slothiesmooth.nyx.feature.vault.basic.domain.VaultRepository
import com.slothiesmooth.nyx.feature.vault.basic.domain.model.VaultImage
import com.slothiesmooth.nyx.shared.data.event.DomainEvent
import com.slothiesmooth.nyx.shared.data.event.DomainEventBus
import com.slothiesmooth.nyx.shared.data.id.StegoImageId
import com.slothiesmooth.nyx.shared.data.result.AppError
import com.slothiesmooth.nyx.shared.data.result.AppResult
import com.slothiesmooth.nyx.shared.data.source.StegoImageRecord
import com.slothiesmooth.nyx.shared.data.source.VaultFileStore
import com.slothiesmooth.nyx.shared.data.source.VaultSource
import com.slothiesmooth.nyx.shared.data.time.Clock
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.coroutines.coroutineContext
import kotlin.time.Instant

private const val ARCHIVE_FAILED = "Failed to archive image"
private const val RESTORE_FAILED = "Failed to restore image"
private const val DELETE_FAILED = "Failed to delete image"

class VaultRepositoryImpl(
    private val vaultSource: VaultSource,
    private val fileStore: VaultFileStore,
    private val clock: Clock,
    private val eventBus: DomainEventBus,
) : VaultRepository {

    override fun observeActive(): Flow<ImmutableList<VaultImage>> =
        vaultSource.observeActive().map { records -> records.map(::toDomain).toImmutableList() }

    override fun observeArchived(): Flow<ImmutableList<VaultImage>> =
        vaultSource.observeArchived().map { records -> records.map(::toDomain).toImmutableList() }

    override suspend fun imageBytes(id: StegoImageId): AppResult<ByteArray> = fileStore.read(id.value)

    override suspend fun archive(id: StegoImageId): AppResult<Unit> = mutate(ARCHIVE_FAILED) {
        vaultSource.setArchived(id.value, archived = true, updatedAt = clock.now().toString())
        eventBus.emit(DomainEvent.StegoImageArchived(id))
    }

    override suspend fun restore(id: StegoImageId): AppResult<Unit> = mutate(RESTORE_FAILED) {
        vaultSource.setArchived(id.value, archived = false, updatedAt = clock.now().toString())
        eventBus.emit(DomainEvent.StegoImageRestored(id))
    }

    override suspend fun softDelete(id: StegoImageId): AppResult<Unit> = mutate(DELETE_FAILED) {
        vaultSource.softDelete(id.value, deletedAt = clock.now().toString())
        eventBus.emit(DomainEvent.StegoImageDeleted(id))
    }

    private fun toDomain(record: StegoImageRecord) = VaultImage(
        id = StegoImageId(record.id),
        name = record.name,
        createdAt = Instant.parse(record.createdAt),
        isArchived = record.isArchived,
    )

    private suspend inline fun mutate(failureMessage: String, block: () -> Unit): AppResult<Unit> {
        // runCatching also traps CancellationException; rethrow it so structured cancellation works.
        val result = runCatching { block() }
        coroutineContext.ensureActive()
        return result.fold(
            onSuccess = { AppResult.Ok(Unit) },
            onFailure = { cause -> AppResult.Err(AppError.Storage(failureMessage, cause)) },
        )
    }
}
