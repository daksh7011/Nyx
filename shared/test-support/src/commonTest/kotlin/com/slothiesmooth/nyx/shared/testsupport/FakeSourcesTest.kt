package com.slothiesmooth.nyx.shared.testsupport

import com.slothiesmooth.nyx.shared.data.result.AppResult
import com.slothiesmooth.nyx.shared.data.source.StegoImageRecord
import com.slothiesmooth.nyx.steganography.PixelImage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FakeSourcesTest {

    private fun record(id: String, archived: Boolean = false, deleted: String? = null) =
        StegoImageRecord(
            id = id,
            name = "n-$id",
            createdAt = "2026-07-13T00:00:00Z",
            updatedAt = "2026-07-13T00:00:00Z",
            deletedAt = deleted,
            isArchived = archived,
        )

    @Test
    fun `active excludes archived and deleted`() = runTest {
        val source = FakeVaultSource()
        source.upsert(record("a"))
        source.upsert(record("b", archived = true))
        source.upsert(record("c", deleted = "2026-07-13T01:00:00Z"))
        val active = source.observeActive().first()
        assertEquals(listOf("a"), active.map { it.id })
        val archived = source.observeArchived().first()
        assertEquals(listOf("b"), archived.map { it.id })
        assertEquals(1, source.countActive())
    }

    @Test
    fun `setArchived and softDelete move records between views`() = runTest {
        val source = FakeVaultSource()
        source.upsert(record("a"))
        source.setArchived("a", archived = true, updatedAt = "2026-07-13T02:00:00Z")
        assertEquals(listOf("a"), source.observeArchived().first().map { it.id })
        source.softDelete("a", deletedAt = "2026-07-13T03:00:00Z")
        assertTrue(source.observeActive().first().isEmpty())
        assertTrue(source.observeArchived().first().isEmpty())
    }

    @Test
    fun `file store writes reads deletes and can force failure`() = runTest {
        val store = FakeVaultFileStore()
        assertTrue(store.write("id", byteArrayOf(1, 2, 3)) is AppResult.Ok)
        assertEquals(listOf<Byte>(1, 2, 3), (store.read("id") as AppResult.Ok).value.toList())
        store.failNextWrite = true
        assertTrue(store.write("id2", byteArrayOf(9)) is AppResult.Err)
        assertTrue(store.deleteAll() is AppResult.Ok)
        assertTrue(store.read("id") is AppResult.Err)
    }

    @Test
    fun `image codec round-trips pixels and forces alpha opaque`() = runTest {
        val codec = FakeImageCodec()
        val original = PixelImage(width = 2, height = 1, pixels = intArrayOf(0x00112233, 0x44556677))
        val encoded = (codec.encodePng(original) as AppResult.Ok).value
        val decoded = (codec.decode(encoded) as AppResult.Ok).value
        assertEquals(2, decoded.width)
        assertEquals(1, decoded.height)
        assertEquals(0xFF112233.toInt(), decoded.pixels[0])
        assertEquals(0xFF556677.toInt(), decoded.pixels[1])
    }
}
