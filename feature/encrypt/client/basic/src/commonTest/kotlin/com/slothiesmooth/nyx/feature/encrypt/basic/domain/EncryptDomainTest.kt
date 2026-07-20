package com.slothiesmooth.nyx.feature.encrypt.basic.domain

import app.cash.turbine.test
import com.slothiesmooth.nyx.crypto.DecryptResult
import com.slothiesmooth.nyx.crypto.DefaultNyxCrypto
import com.slothiesmooth.nyx.feature.encrypt.basic.domain.usecase.EncryptMessageUseCase
import com.slothiesmooth.nyx.feature.encrypt.basic.domain.usecase.SaveToVaultUseCase
import com.slothiesmooth.nyx.shared.data.event.DefaultDomainEventBus
import com.slothiesmooth.nyx.shared.data.event.DomainEvent
import com.slothiesmooth.nyx.shared.data.id.IdGenerator
import com.slothiesmooth.nyx.shared.data.id.StegoImageId
import com.slothiesmooth.nyx.shared.data.result.AppResult
import com.slothiesmooth.nyx.shared.testsupport.FakeImageCodec
import com.slothiesmooth.nyx.shared.testsupport.FakeVaultFileStore
import com.slothiesmooth.nyx.shared.testsupport.FakeVaultSource
import com.slothiesmooth.nyx.shared.testsupport.time.FakeClock
import com.slothiesmooth.nyx.steganography.PixelImage
import com.slothiesmooth.nyx.steganography.Steganography
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

private const val OPAQUE = 0xFF000000.toInt()

class EncryptDomainTest {

    private val codec = FakeImageCodec()
    private val crypto = DefaultNyxCrypto()
    private val stego = Steganography()

    private suspend fun coverBytes(width: Int, height: Int): ByteArray {
        val pixels = IntArray(width * height) { OPAQUE or (it and 0x00FFFFFF) }
        return (codec.encodePng(PixelImage(width, height, pixels)) as AppResult.Ok).value
    }

    @Test
    fun `encrypt then manual stego+crypto decode recovers the message`() = runTest {
        val useCase = EncryptMessageUseCase(crypto, stego, codec)
        val png = useCase(coverBytes(96, 96), "meet me at dawn", "hunter2")
        assertTrue(png is EncryptOutcome.Success)
        val decoded = (codec.decode(png.pngBytes) as AppResult.Ok).value
        val blob = stego.decode(listOf(decoded))!!
        val result = crypto.decrypt(blob, "hunter2")
        assertEquals(DecryptResult.Success("meet me at dawn"), result)
    }

    @Test
    fun `encrypt reports TooLarge with capacity counts when the message exceeds capacity`() = runTest {
        val useCase = EncryptMessageUseCase(crypto, stego, codec)
        val result = useCase(coverBytes(4, 4), "x".repeat(500), "pw")
        assertTrue(result is EncryptOutcome.TooLarge)
        assertTrue(result.requiredChars > result.availableChars)
    }

    @Test
    fun `estimateMaxMessageChars grows with image area and is non-negative`() {
        assertEquals(0, estimateMaxMessageChars(2, 2))
        assertTrue(estimateMaxMessageChars(256, 256) > estimateMaxMessageChars(64, 64))
    }

    @Test
    fun `save writes the file and upserts an auto-named record and emits StegoImageStored`() = runTest {
        val source = FakeVaultSource()
        val store = FakeVaultFileStore()
        val bus = DefaultDomainEventBus()
        val idGen = object : IdGenerator { override fun newId() = "abcdef1234567890" }
        val useCase = SaveToVaultUseCase(source, store, idGen, FakeClock(Instant.parse("2026-07-13T12:00:00Z")), bus)

        bus.events.test {
            val result = useCase(byteArrayOf(1, 2, 3), name = "")
            assertTrue(result is AppResult.Ok)
            assertEquals("abcdef1234567890", result.value.value)
            assertEquals(DomainEvent.StegoImageStored(StegoImageId("abcdef1234567890")), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        val record = source.observeActive().first().single()
        assertEquals("nyx-abcdef12.png", record.name)
        assertTrue(store.read("abcdef1234567890") is AppResult.Ok)
    }

    @Test
    fun `save rolls the file back and errors when the row insert fails`() = runTest {
        val source = FakeVaultSource().apply { failNextUpsert = true }
        val store = FakeVaultFileStore()
        val idGen = object : IdGenerator { override fun newId() = "id0000000000" }
        val useCase = SaveToVaultUseCase(
            source,
            store,
            idGen,
            FakeClock(Instant.parse("2026-07-13T12:00:00Z")),
            DefaultDomainEventBus(),
        )
        val result = useCase(byteArrayOf(9), name = "")
        assertTrue(result is AppResult.Err)
        assertTrue(store.read("id0000000000") is AppResult.Err)
    }
}
