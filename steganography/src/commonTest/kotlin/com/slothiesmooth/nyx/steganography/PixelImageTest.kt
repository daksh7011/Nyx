package com.slothiesmooth.nyx.steganography

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PixelImageTest {
    @Test
    fun `holds its dimensions and pixel data`() {
        val pixels = intArrayOf(0xFF112233.toInt(), 0xFF445566.toInt(), 1, 2)
        val image = PixelImage(width = 2, height = 2, pixels = pixels)
        assertEquals(2, image.width)
        assertEquals(2, image.height)
        assertContentEquals(pixels, image.pixels)
    }

    @Test
    fun `rejects a pixel array whose size is not width times height`() {
        assertFailsWith<IllegalArgumentException> {
            PixelImage(width = 2, height = 2, pixels = intArrayOf(1, 2, 3))
        }
    }
}
