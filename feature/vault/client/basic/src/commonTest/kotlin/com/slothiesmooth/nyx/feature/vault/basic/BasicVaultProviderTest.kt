package com.slothiesmooth.nyx.feature.vault.basic

import app.cash.turbine.test
import com.slothiesmooth.nyx.shared.data.event.DefaultDomainEventBus
import com.slothiesmooth.nyx.shared.data.source.StegoImageRecord
import com.slothiesmooth.nyx.shared.testsupport.FakeShareSource
import com.slothiesmooth.nyx.shared.testsupport.FakeVaultFileStore
import com.slothiesmooth.nyx.shared.testsupport.FakeVaultSource
import com.slothiesmooth.nyx.shared.testsupport.time.FakeClock
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class BasicVaultProviderTest {

    private fun record(id: String) = StegoImageRecord(
        id = id,
        name = "nyx-$id.png",
        createdAt = "2026-07-10T09:00:00Z",
        updatedAt = "2026-07-10T09:00:00Z",
        deletedAt = null,
        isArchived = false,
    )

    private fun provider(vaultSource: FakeVaultSource) = BasicVaultProvider(
        vaultSource = vaultSource,
        fileStore = FakeVaultFileStore(),
        clock = FakeClock(Instant.parse("2026-07-13T12:00:00Z")),
        eventBus = DefaultDomainEventBus(),
        shareSource = FakeShareSource(),
    )

    @Test
    fun `observeActiveCount reflects the live active-record count from vaultSource`() = runTest {
        val vaultSource = FakeVaultSource()

        provider(vaultSource).observeActiveCount().test {
            assertEquals(0, awaitItem())
            vaultSource.upsert(record("a"))
            assertEquals(1, awaitItem())
            vaultSource.upsert(record("b"))
            assertEquals(2, awaitItem())
            vaultSource.setArchived("a", archived = true, updatedAt = "2026-07-13T12:00:00Z")
            assertEquals(1, awaitItem())
        }
    }
}
