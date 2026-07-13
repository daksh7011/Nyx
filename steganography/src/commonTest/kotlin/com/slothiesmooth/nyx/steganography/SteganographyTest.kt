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
}

private fun solidImage(width: Int, height: Int, argb: Int): PixelImage =
    PixelImage(width, height, IntArray(width * height) { argb })
