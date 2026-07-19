package com.slothiesmooth.nyx.feature.decrypt.basic.domain

import com.slothiesmooth.nyx.crypto.DefaultNyxCrypto
import com.slothiesmooth.nyx.feature.decrypt.basic.domain.usecase.DecryptMessageUseCase
import com.slothiesmooth.nyx.feature.decrypt.basic.domain.usecase.LoadVaultImageBytesUseCase
import com.slothiesmooth.nyx.shared.data.id.StegoImageId
import com.slothiesmooth.nyx.shared.data.result.AppResult
import com.slothiesmooth.nyx.shared.testsupport.FakeImageCodec
import com.slothiesmooth.nyx.shared.testsupport.FakeVaultFileStore
import com.slothiesmooth.nyx.steganography.PixelImage
import com.slothiesmooth.nyx.steganography.Steganography
import com.slothiesmooth.nyx.steganography.StegoEncodeResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private const val OPAQUE = 0xFF000000.toInt()

class DecryptDomainTest {

    private val codec = FakeImageCodec()
    private val crypto = DefaultNyxCrypto()
    private val stego = Steganography()

    private suspend fun stegoImageBytes(message: String, password: String): ByteArray {
        val cover = PixelImage(96, 96, IntArray(96 * 96) { OPAQUE })
        val blob = crypto.encrypt(message, password)
        val encoded = stego.encode(listOf(cover), blob) as StegoEncodeResult.Success
        return (codec.encodePng(encoded.images.first()) as AppResult.Ok).value
    }

    private suspend fun plainImageBytes(): ByteArray =
        (codec.encodePng(PixelImage(8, 8, IntArray(64) { OPAQUE })) as AppResult.Ok).value

    @Test
    fun `correct password reveals the plaintext`() = runTest {
        val bytes = stegoImageBytes("the eagle lands at noon", "correct-horse")
        val outcome = DecryptMessageUseCase(crypto, stego, codec)(bytes, "correct-horse")
        assertEquals(DecryptOutcome.Success("the eagle lands at noon"), outcome)
    }

    @Test
    fun `wrong password is distinct from a malformed image`() = runTest {
        val bytes = stegoImageBytes("secret", "right")
        val outcome = DecryptMessageUseCase(crypto, stego, codec)(bytes, "wrong")
        assertEquals(DecryptOutcome.WrongPasswordOrTampered, outcome)
    }

    @Test
    fun `image without a hidden payload reports no hidden message`() = runTest {
        val outcome = DecryptMessageUseCase(crypto, stego, codec)(plainImageBytes(), "anything")
        assertEquals(DecryptOutcome.NoHiddenMessage, outcome)
    }

    @Test
    fun `load vault image bytes reads the file store`() = runTest {
        val store = FakeVaultFileStore().apply { write("a", byteArrayOf(5, 6)) }
        val result = LoadVaultImageBytesUseCase(store)(StegoImageId("a"))
        assertIs<AppResult.Ok<ByteArray>>(result)
        assertTrue(result.value.contentEquals(byteArrayOf(5, 6)))
    }
}
