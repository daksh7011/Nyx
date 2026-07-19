package com.slothiesmooth.nyx.feature.vault.basic.presentation

import com.slothiesmooth.nyx.feature.vault.basic.data.VaultRepositoryImpl
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.GetImageBytesUseCase
import com.slothiesmooth.nyx.feature.vault.basic.presentation.list.buildVaultRows
import com.slothiesmooth.nyx.shared.data.event.DefaultDomainEventBus
import com.slothiesmooth.nyx.shared.data.source.StegoImageRecord
import com.slothiesmooth.nyx.shared.testsupport.FakeVaultFileStore
import com.slothiesmooth.nyx.shared.testsupport.FakeVaultSource
import com.slothiesmooth.nyx.shared.testsupport.time.FakeClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class VaultRowsTest {

    @Test
    fun `formatVaultDate renders abbreviated month day and year`() {
        val label = formatVaultDate(Instant.parse("2026-07-13T12:00:00Z"), TimeZone.UTC)
        assertEquals("Jul 13, 2026", label)
    }

    @Test
    fun `buildVaultRows maps every image to a render-ready row and decodes its bytes`() = runTest {
        val source = FakeVaultSource().apply {
            upsert(StegoImageRecord("a", "nyx-a.png", "2026-07-13T12:00:00Z", "2026-07-13T12:00:00Z", null, false))
        }
        val store = FakeVaultFileStore().apply { write("a", byteArrayOf(4, 2)) }
        val repo = VaultRepositoryImpl(
            source,
            store,
            FakeClock(Instant.parse("2026-07-13T12:00:00Z")),
            DefaultDomainEventBus(),
        )
        val decodedByteCounts = mutableListOf<Int>()

        val rows = buildVaultRows(
            images = repo.observeActive().first(),
            getImageBytes = GetImageBytesUseCase(repo),
            zone = TimeZone.UTC,
            decode = { bytes ->
                decodedByteCounts.add(bytes.size)
                null
            },
        )

        assertEquals(1, rows.size)
        assertEquals("nyx-a.png", rows[0].name)
        assertEquals("Jul 13, 2026", rows[0].createdLabel)
        assertNull(rows[0].thumbnail)
        assertEquals(listOf(2), decodedByteCounts)
    }
}
