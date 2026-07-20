package com.slothiesmooth.nyx.web

import com.slothiesmooth.nyx.shared.data.source.StegoImageRecord
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class InMemoryVaultSourceTest {

    private fun record(id: String, createdAt: String, archived: Boolean = false): StegoImageRecord =
        StegoImageRecord(
            id = id,
            name = "$id.png",
            createdAt = createdAt,
            updatedAt = createdAt,
            deletedAt = null,
            isArchived = archived,
        )

    @Test
    fun `active is newest-first and excludes archived; archived view holds the rest`() = runTest {
        val source = InMemoryVaultSource()
        source.upsert(record("a", "2026-01-01T00:00:00Z"))
        source.upsert(record("b", "2026-02-01T00:00:00Z"))
        source.upsert(record("c", "2026-03-01T00:00:00Z", archived = true))

        assertEquals(listOf("b", "a"), source.observeActive().first().map { it.id })
        assertEquals(listOf("c"), source.observeArchived().first().map { it.id })
    }

    @Test
    fun `archiving moves a record between views and purge clears everything`() = runTest {
        val source = InMemoryVaultSource()
        source.upsert(record("a", "2026-01-01T00:00:00Z"))

        source.setArchived("a", archived = true, updatedAt = "2026-04-01T00:00:00Z")
        assertEquals(0, source.observeActive().first().size)
        assertEquals(listOf("a"), source.observeArchived().first().map { it.id })

        source.purgeAll()
        assertEquals(0, source.countActive())
    }
}
