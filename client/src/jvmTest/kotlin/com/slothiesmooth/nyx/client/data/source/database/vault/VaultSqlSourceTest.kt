package com.slothiesmooth.nyx.client.data.source.database.vault

import app.cash.sqldelight.async.coroutines.synchronous
import com.slothiesmooth.nyx.client.data.source.database.sqldelight.SqlDelightSource
import com.slothiesmooth.nyx.client.data.sqldelight.NyxDb
import com.slothiesmooth.nyx.shared.data.source.StegoImageRecord
import com.slothiesmooth.nyx.shared.testsupport.db.createTestSqlDriver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VaultSqlSourceTest {

    private fun newVault(scope: CoroutineScope, scheduler: TestCoroutineScheduler): VaultSqlSource {
        val driver = createTestSqlDriver(NyxDb.Schema.synchronous())
        val source = SqlDelightSource(driver, scope)
        return VaultSqlSource(source, UnconfinedTestDispatcher(scheduler))
    }

    private fun record(
        id: String,
        name: String = "Secret",
        archived: Boolean = false,
        deletedAt: String? = null,
    ) = StegoImageRecord(
        id = id,
        name = name,
        createdAt = "2026-07-13T00:00:00Z",
        updatedAt = "2026-07-13T00:00:00Z",
        deletedAt = deletedAt,
        isArchived = archived,
    )

    @Test
    fun `upsert then observeActive emits the record`() = runTest {
        val vault = newVault(backgroundScope, testScheduler)
        val stored = record("id-1")

        vault.upsert(stored)

        assertEquals(listOf(stored), vault.observeActive().first())
    }

    @Test
    fun `upsert with an existing id updates in place`() = runTest {
        val vault = newVault(backgroundScope, testScheduler)
        vault.upsert(record("id-1", name = "Old"))

        vault.upsert(record("id-1", name = "New"))

        val active = vault.observeActive().first()
        assertEquals(1, active.size)
        assertEquals("New", active.single().name)
    }

    @Test
    fun `getById returns the record or null`() = runTest {
        val vault = newVault(backgroundScope, testScheduler)
        val stored = record("id-1")
        vault.upsert(stored)

        assertEquals(stored, vault.getById("id-1"))
        assertNull(vault.getById("missing"))
    }

    @Test
    fun `setArchived moves the record from active to archived`() = runTest {
        val vault = newVault(backgroundScope, testScheduler)
        vault.upsert(record("id-1"))

        vault.setArchived("id-1", archived = true, updatedAt = "2026-07-14T00:00:00Z")

        assertTrue(vault.observeActive().first().isEmpty())
        assertEquals(listOf("id-1"), vault.observeArchived().first().map { it.id })
    }

    @Test
    fun `softDelete tombstones the record so it is excluded from active`() = runTest {
        val vault = newVault(backgroundScope, testScheduler)
        vault.upsert(record("id-1"))

        vault.softDelete("id-1", deletedAt = "2026-07-14T00:00:00Z")

        assertTrue(vault.observeActive().first().isEmpty())
        assertEquals(0, vault.countActive())
    }

    @Test
    fun `countActive counts only live non-archived rows`() = runTest {
        val vault = newVault(backgroundScope, testScheduler)
        vault.upsert(record("id-1"))
        vault.upsert(record("id-2"))
        assertEquals(2, vault.countActive())

        vault.setArchived("id-2", archived = true, updatedAt = "2026-07-14T00:00:00Z")
        assertEquals(1, vault.countActive())
    }

    @Test
    fun `purgeAll removes every row including tombstones`() = runTest {
        val vault = newVault(backgroundScope, testScheduler)
        vault.upsert(record("id-1"))
        vault.upsert(record("id-2", archived = true))
        vault.softDelete("id-1", deletedAt = "2026-07-14T00:00:00Z")

        vault.purgeAll()

        assertEquals(0, vault.countActive())
        assertTrue(vault.observeActive().first().isEmpty())
        assertTrue(vault.observeArchived().first().isEmpty())
    }
}
