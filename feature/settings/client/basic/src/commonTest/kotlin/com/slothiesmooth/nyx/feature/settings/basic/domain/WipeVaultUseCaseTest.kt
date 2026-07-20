package com.slothiesmooth.nyx.feature.settings.basic.domain

import com.slothiesmooth.nyx.feature.settings.basic.domain.usecase.WipeVaultUseCase
import com.slothiesmooth.nyx.shared.data.event.DefaultDomainEventBus
import com.slothiesmooth.nyx.shared.data.event.DomainEvent
import com.slothiesmooth.nyx.shared.data.result.AppError
import com.slothiesmooth.nyx.shared.data.result.AppResult
import com.slothiesmooth.nyx.shared.data.source.StegoImageRecord
import com.slothiesmooth.nyx.shared.data.source.VaultFileStore
import com.slothiesmooth.nyx.shared.testsupport.FakeVaultFileStore
import com.slothiesmooth.nyx.shared.testsupport.FakeVaultSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WipeVaultUseCaseTest {

    @Test
    fun `wipe purges rows and files and emits VaultWiped`() = runTest {
        val source = FakeVaultSource().apply {
            upsert(StegoImageRecord("a", "nyx-a.png", "2026-07-13T00:00:00Z", "2026-07-13T00:00:00Z", null, false))
        }
        val store = FakeVaultFileStore().apply { write("a", byteArrayOf(1)) }
        val bus = DefaultDomainEventBus()
        val events = mutableListOf<DomainEvent>()
        val job = CoroutineScope(Dispatchers.Unconfined).launch { bus.events.collect { events.add(it) } }

        val result = WipeVaultUseCase(source, store, bus)()

        assertTrue(result is AppResult.Ok)
        assertTrue(source.observeActive().first().isEmpty())
        assertTrue(store.read("a") is AppResult.Err)
        assertEquals(DomainEvent.VaultWiped, events.single())
        job.cancel()
    }

    @Test
    fun `wipe reports a storage error when file deletion fails and does not emit`() = runTest {
        val failingStore = FailingDeleteAllStore()
        val bus = DefaultDomainEventBus()
        val events = mutableListOf<DomainEvent>()
        val job = CoroutineScope(Dispatchers.Unconfined).launch { bus.events.collect { events.add(it) } }

        val result = WipeVaultUseCase(FakeVaultSource(), failingStore, bus)()

        assertTrue(result is AppResult.Err)
        assertTrue(events.isEmpty())
        job.cancel()
    }
}

private class FailingDeleteAllStore : VaultFileStore {
    override suspend fun write(id: String, bytes: ByteArray) = AppResult.Ok(Unit)
    override suspend fun read(id: String) = AppResult.Err(AppError.NotFound)
    override suspend fun delete(id: String) = AppResult.Ok(Unit)
    override suspend fun deleteAll() = AppResult.Err(AppError.Storage("disk error"))
}
