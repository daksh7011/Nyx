package com.slothiesmooth.nyx.feature.settings.basic.domain.usecase

import com.slothiesmooth.nyx.shared.data.event.DomainEvent
import com.slothiesmooth.nyx.shared.data.event.DomainEventBus
import com.slothiesmooth.nyx.shared.data.result.AppError
import com.slothiesmooth.nyx.shared.data.result.AppResult
import com.slothiesmooth.nyx.shared.data.source.VaultFileStore
import com.slothiesmooth.nyx.shared.data.source.VaultSource
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

private const val WIPE_FAILED = "Failed to wipe the vault"

/** Hard delete: removes every file AND every row (bypasses tombstones), then emits [DomainEvent.VaultWiped]. */
class WipeVaultUseCase(
    private val vaultSource: VaultSource,
    private val fileStore: VaultFileStore,
    private val eventBus: DomainEventBus,
) {
    suspend operator fun invoke(): AppResult<Unit> {
        when (val files = fileStore.deleteAll()) {
            is AppResult.Ok -> Unit
            is AppResult.Err -> return files
        }
        // runCatching also traps CancellationException; rethrow it so structured cancellation works.
        val result = runCatching {
            vaultSource.purgeAll()
            eventBus.emit(DomainEvent.VaultWiped)
        }
        currentCoroutineContext().ensureActive()
        return result.fold(
            onSuccess = { AppResult.Ok(Unit) },
            onFailure = { cause -> AppResult.Err(AppError.Storage(WIPE_FAILED, cause)) },
        )
    }
}
