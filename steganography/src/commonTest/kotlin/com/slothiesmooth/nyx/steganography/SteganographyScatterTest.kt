package com.slothiesmooth.nyx.steganography

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private const val COVER_SIDE = 200
private const val COVER_ARGB = 0xFF000000.toInt() // low 2 bits of R/G/B start at 0, so any non-zero chunk shows as a diff.
private const val SCATTER_PAYLOAD = "Nyx12" // 5 bytes, per the brief's "small payload" spread test.
private const val CHANNELS_PER_PIXEL = 3

// Total embeddable channels for the 200x200 opaque cover: 200 * 200 * 3 = 120_000.
private const val CHANNEL_COUNT = COVER_SIDE.toLong() * COVER_SIDE.toLong() * CHANNELS_PER_PIXEL

// A third of the channel range: used to prove changes land near both the start AND the end,
// not just packed into the first few channels the way sequential embedding would.
private const val FIRST_THIRD_BOUND = CHANNEL_COUNT / 3
private const val LAST_THIRD_BOUND = CHANNEL_COUNT * 2 / 3

// Sequential embedding of ~44 chunks into a 120_000-channel cover would leave one contiguous
// gap of ~119_950 channels. A scattered permutation over such a large domain should keep every
// gap well under half the channel range; this is a coarse but decisive discriminator between
// "packed at the start" and "spread across the whole image".
private const val MAX_ALLOWED_GAP = CHANNEL_COUNT / 2

class SteganographyScatterTest {

    @Test
    fun `scatters changed channels across the whole cover instead of packing them at the start`() = runTest {
        val stego = Steganography()
        val cover = solidCover()
        val result = assertIs<StegoEncodeResult.Success>(stego.encode(listOf(cover), SCATTER_PAYLOAD))
        val stegoImage = result.images.single()

        val changedChannels = changedChannelIndices(cover, stegoImage)
        assertTrue(changedChannels.isNotEmpty(), "expected at least one changed channel")
        assertTrue(
            changedChannels.any { it < FIRST_THIRD_BOUND },
            "expected a changed channel in the first third [0, $FIRST_THIRD_BOUND); got $changedChannels",
        )
        assertTrue(
            changedChannels.any { it >= LAST_THIRD_BOUND },
            "expected a changed channel in the last third [$LAST_THIRD_BOUND, $CHANNEL_COUNT); got $changedChannels",
        )
        assertTrue(
            maxGap(changedChannels) < MAX_ALLOWED_GAP,
            "expected max gap between changed channels below $MAX_ALLOWED_GAP; got ${maxGap(changedChannels)}",
        )
    }

    @Test
    fun `encoding the same payload and cover twice is byte-identical`() = runTest {
        val stego = Steganography()
        val cover = solidCover()
        val first = assertIs<StegoEncodeResult.Success>(stego.encode(listOf(cover), SCATTER_PAYLOAD))
        val second = assertIs<StegoEncodeResult.Success>(stego.encode(listOf(cover), SCATTER_PAYLOAD))
        assertContentEquals(first.images.single().pixels, second.images.single().pixels)
    }

    private fun solidCover(): PixelImage =
        PixelImage(width = COVER_SIDE, height = COVER_SIDE, pixels = IntArray(COVER_SIDE * COVER_SIDE) { COVER_ARGB })

    private fun changedChannelIndices(cover: PixelImage, stego: PixelImage): List<Long> {
        val changed = ArrayList<Long>()
        for (pixelIndex in cover.pixels.indices) {
            val before = cover.pixels[pixelIndex]
            val after = stego.pixels[pixelIndex]
            for (colorSlot in 0 until CHANNELS_PER_PIXEL) {
                val shift = (2 - colorSlot) * 8
                val beforeChunk = (before ushr shift) and 0x3
                val afterChunk = (after ushr shift) and 0x3
                if (beforeChunk != afterChunk) {
                    changed.add(pixelIndex.toLong() * CHANNELS_PER_PIXEL + colorSlot)
                }
            }
        }
        return changed.sorted()
    }

    private fun maxGap(sortedChannels: List<Long>): Long {
        var max = sortedChannels.first()
        for (index in 0 until sortedChannels.size - 1) {
            max = maxOf(max, sortedChannels[index + 1] - sortedChannels[index])
        }
        max = maxOf(max, CHANNEL_COUNT - 1 - sortedChannels.last())
        return max
    }
}
