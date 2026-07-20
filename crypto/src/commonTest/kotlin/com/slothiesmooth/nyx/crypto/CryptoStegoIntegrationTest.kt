package com.slothiesmooth.nyx.crypto

import com.slothiesmooth.nyx.steganography.PixelImage
import com.slothiesmooth.nyx.steganography.Steganography
import com.slothiesmooth.nyx.steganography.StegoEncodeResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class CryptoStegoIntegrationTest {

    @Test
    fun `encrypt then hide then reveal then decrypt recovers the original message`() = runTest {
        val crypto = DefaultNyxCrypto()
        val stego = Steganography()
        val secret = "Meet at the north pier 🌊 at 04:15"
        val password = "correct horse battery staple"

        val blob = crypto.encrypt(secret, password)
        // The Base64 blob alphabet excludes the marker characters, so no false end-marker match.
        val cover = PixelImage(width = 96, height = 96, pixels = IntArray(96 * 96) { 0xFF335577.toInt() })

        val encoded = assertIs<StegoEncodeResult.Success>(stego.encode(listOf(cover), blob))
        val revealed = stego.decode(encoded.images)
        assertNotNull(revealed)
        assertEquals(blob, revealed)
        assertEquals(DecryptResult.Success(secret), crypto.decrypt(revealed, password))
    }
}
