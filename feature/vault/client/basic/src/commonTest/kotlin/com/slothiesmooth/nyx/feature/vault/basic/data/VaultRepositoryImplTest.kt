package com.slothiesmooth.nyx.feature.vault.basic.data

import com.slothiesmooth.nyx.shared.data.event.DefaultDomainEventBus
import com.slothiesmooth.nyx.shared.data.event.DomainEvent
import com.slothiesmooth.nyx.shared.data.id.StegoImageId
import com.slothiesmooth.nyx.shared.data.result.AppResult
import com.slothiesmooth.nyx.shared.data.source.StegoImageRecord
import com.slothiesmooth.nyx.shared.testsupport.FakeVaultFileStore
import com.slothiesmooth.nyx.shared.testsupport.FakeVaultSource
import com.slothiesmooth.nyx.shared.testsupport.time.FakeClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class VaultRepositoryImplTest {

    private val fixed = Instant.parse("2026-07-13T12:00:00Z")

    private fun repo(source: FakeVaultSource, store: FakeVaultFileStore, bus: DefaultDomainEventBus) =
        VaultRepositoryImpl(vaultSource = source, fileStore = store, clock = FakeClock(fixed), eventBus = bus)

    private fun record(id: String, createdAt: String = "2026-07-10T09:00:00Z") =
        StegoImageRecord(
            id = id,
            name = "nyx-$id.png",
            createdAt = createdAt,
            updatedAt = createdAt,
            deletedAt = null,
            isArchived = false,
        )

    @Test
    fun `observeActive maps records to domain and parses ISO instant`() = runTest {
        val source = FakeVaultSource().apply { upsert(record("a")) }
        val images = repo(source, FakeVaultFileStore(), DefaultDomainEventBus()).observeActive().first()
        assertEquals(1, images.size)
        assertEquals(StegoImageId("a"), images[0].id)
        assertEquals("nyx-a.png", images[0].name)
        assertEquals(Instant.parse("2026-07-10T09:00:00Z"), images[0].createdAt)
        assertTrue(!images[0].isArchived)
    }

    @Test
    fun `imageBytes reads from the file store`() = runTest {
        val store = FakeVaultFileStore().apply { write("a", byteArrayOf(7, 8, 9)) }
        val result = repo(FakeVaultSource(), store, DefaultDomainEventBus()).imageBytes(StegoImageId("a"))
        assertTrue(result is AppResult.Ok)
        assertEquals(listOf<Byte>(7, 8, 9), result.value.toList())
    }

    @Test
    fun `archive stamps updatedAt from clock and emits StegoImageArchived`() = runTest {
        val source = FakeVaultSource().apply { upsert(record("a")) }
        val bus = DefaultDomainEventBus()
        val events = mutableListOf<DomainEvent>()
        val collector = CoroutineScope(Dispatchers.Unconfined)
        val job = collector.launch { bus.events.collect { events.add(it) } }
        val result = repo(source, FakeVaultFileStore(), bus).archive(StegoImageId("a"))
        assertTrue(result is AppResult.Ok)
        assertTrue(source.observeArchived().first().any { it.id == "a" && it.updatedAt == fixed.toString() })
        assertEquals(DomainEvent.StegoImageArchived(StegoImageId("a")), events.single())
        job.cancel()
    }

    @Test
    fun `restore un-archives and emits StegoImageRestored`() = runTest {
        val source = FakeVaultSource().apply {
            upsert(record("a"))
            setArchived("a", true, "2026-07-11T00:00:00Z")
        }
        val result = repo(source, FakeVaultFileStore(), DefaultDomainEventBus()).restore(StegoImageId("a"))
        assertTrue(result is AppResult.Ok)
        assertTrue(source.observeActive().first().any { it.id == "a" })
    }

    @Test
    fun `softDelete tombstones the row and emits StegoImageDeleted`() = runTest {
        val source = FakeVaultSource().apply { upsert(record("a")) }
        val result = repo(source, FakeVaultFileStore(), DefaultDomainEventBus()).softDelete(StegoImageId("a"))
        assertTrue(result is AppResult.Ok)
        assertTrue(source.observeActive().first().isEmpty())
    }
}
