package com.slothiesmooth.nyx.steganography

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class SteganographyTest {

    @Test
    fun `encode reports required and available bits when the cover is too small`() = runTest {
        val stego = Steganography()
        // 2x2 cover = 4 px * 3 channels = 12 channels -> 24 usable bits.
        val cover = solidImage(width = 2, height = 2, argb = 0xFF000000.toInt())
        // frame "@!#" + "X" + "#!@" = 7 bytes -> 56 required bits.
        val exceeded = assertIs<StegoEncodeResult.CapacityExceeded>(stego.encode(listOf(cover), "X"))
        assertEquals(56L, exceeded.requiredBits)
        assertEquals(24L, exceeded.availableBits)
    }

    @Test
    fun `round trips a payload through a single cover`() = runTest {
        val stego = Steganography()
        val cover = solidImage(width = 64, height = 64, argb = 0xFF3366AA.toInt())
        val secret = "Hello, Nyx!"
        val result = assertIs<StegoEncodeResult.Success>(stego.encode(listOf(cover), secret))
        assertEquals(secret, stego.decode(result.images))
    }

    @Test
    fun `spans a payload across multiple covers`() = runTest {
        val stego = Steganography()
        // Each cover: 6x2 = 36 channels = 9 frame-byte capacity.
        // frame "@!#" + "SPANNING" + "#!@" = 14 bytes -> needs two covers.
        val first = solidImage(width = 6, height = 2, argb = 0xFF101010.toInt())
        val second = solidImage(width = 6, height = 2, argb = 0xFF202020.toInt())
        val secret = "SPANNING"
        val result = assertIs<StegoEncodeResult.Success>(stego.encode(listOf(first, second), secret))
        assertEquals(2, result.images.size)
        assertEquals(secret, stego.decode(result.images))
        // The first cover alone cannot hold the whole frame -> no end marker -> null.
        assertNull(stego.decode(listOf(result.images.first())))
    }

    @Test
    fun `returns null when no framed payload is present`() = runTest {
        val stego = Steganography()
        // Low 2 bits are 0 -> decoded bytes are 0x00, never the start marker.
        val plain = solidImage(width = 16, height = 16, argb = 0xFF000000.toInt())
        assertNull(stego.decode(listOf(plain)))
    }

    @Test
    fun `fills a cover exactly at capacity`() = runTest {
        val stego = Steganography()
        // 6x2 = 36 channels = 72 usable bits = 9 frame bytes.
        // frame "@!#" + "ABC" + "#!@" = 9 bytes -> exact fit.
        val cover = solidImage(width = 6, height = 2, argb = 0xFF204060.toInt())
        val result = assertIs<StegoEncodeResult.Success>(stego.encode(listOf(cover), "ABC"))
        assertEquals("ABC", stego.decode(result.images))
    }

    @Test
    fun `rejects a payload one byte over capacity`() = runTest {
        val stego = Steganography()
        val cover = solidImage(width = 6, height = 2, argb = 0xFF204060.toInt())
        // frame "@!#" + "ABCD" + "#!@" = 10 bytes -> 80 required bits > 72 available.
        val exceeded =
            assertIs<StegoEncodeResult.CapacityExceeded>(stego.encode(listOf(cover), "ABCD"))
        assertEquals(80L, exceeded.requiredBits)
        assertEquals(72L, exceeded.availableBits)
    }

    @Test
    fun `changes only the low two bits of each RGB channel`() = runTest {
        val stego = Steganography()
        val cover = solidImage(width = 32, height = 32, argb = 0xFF7F7F7F.toInt())
        val result = assertIs<StegoEncodeResult.Success>(stego.encode(listOf(cover), "delta check"))
        val output = result.images.single()
        // Alpha + the high 6 bits of R, G, B must be byte-identical to the cover.
        val preserveMask = 0xFFFCFCFC.toInt()
        cover.pixels.indices.forEach { index ->
            assertEquals(
                cover.pixels[index] and preserveMask,
                output.pixels[index] and preserveMask,
                "pixel $index changed outside the low 2 bits of R/G/B",
            )
        }
    }

    @Test
    fun `never modifies the alpha channel`() = runTest {
        val stego = Steganography()
        // Semi-transparent cover: the algorithm must neither touch nor force alpha opaque.
        val cover = solidImage(width = 32, height = 32, argb = 0x80112233.toInt())
        val result = assertIs<StegoEncodeResult.Success>(stego.encode(listOf(cover), "keep alpha"))
        result.images.single().pixels.forEach { pixel ->
            assertEquals(0x80, (pixel ushr 24) and 0xFF, "alpha byte must stay 0x80")
        }
    }

    @Test
    fun `round trips a unicode payload`() = runTest {
        val stego = Steganography()
        val cover = solidImage(width = 64, height = 64, argb = 0xFF446688.toInt())
        val secret = "Rendezvous 🦊🔒 at 07:30"
        val result = assertIs<StegoEncodeResult.Success>(stego.encode(listOf(cover), secret))
        assertEquals(secret, stego.decode(result.images))
    }

    @Test
    fun `round trips an empty payload`() = runTest {
        val stego = Steganography()
        val cover = solidImage(width = 8, height = 8, argb = 0xFF123456.toInt())
        val result = assertIs<StegoEncodeResult.Success>(stego.encode(listOf(cover), ""))
        assertEquals("", stego.decode(result.images))
    }
}

private fun solidImage(width: Int, height: Int, argb: Int): PixelImage =
    PixelImage(width, height, IntArray(width * height) { argb })
