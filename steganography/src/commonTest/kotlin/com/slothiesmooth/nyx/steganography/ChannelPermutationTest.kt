package com.slothiesmooth.nyx.steganography

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val SMALL_CHANNEL_COUNT = 10L
private const val ODD_CHANNEL_COUNT = 17L
private const val POWER_OF_TWO_CHANNEL_COUNT = 64L
private const val LARGE_CHANNEL_COUNT = 1000L

/**
 * Verifies [ChannelPermutation] is a true bijection on `[0, channelCount)`: every input index
 * maps to a distinct, in-range output. This is the load-bearing correctness fact the whole
 * scatter scheme depends on -- if it fails, encode/decode cannot be symmetric.
 */
class ChannelPermutationTest {

    @Test
    fun `is a bijection over a small channel count`() = assertBijection(SMALL_CHANNEL_COUNT)

    @Test
    fun `is a bijection over an odd channel count`() = assertBijection(ODD_CHANNEL_COUNT)

    @Test
    fun `is a bijection over a power-of-two channel count`() = assertBijection(POWER_OF_TWO_CHANNEL_COUNT)

    @Test
    fun `is a bijection over a large channel count`() = assertBijection(LARGE_CHANNEL_COUNT)

    private fun assertBijection(channelCount: Long) {
        val permutation = ChannelPermutation(channelCount)
        val seen = BooleanArray(channelCount.toInt())
        for (index in 0 until channelCount) {
            val mapped = permutation.map(index)
            assertTrue(
                mapped in 0 until channelCount,
                "map($index) = $mapped is out of range [0, $channelCount)",
            )
            assertTrue(!seen[mapped.toInt()], "map($index) = $mapped duplicates an earlier mapping")
            seen[mapped.toInt()] = true
        }
        assertEquals(channelCount.toInt(), seen.count { it }, "not every channel in [0, $channelCount) was hit")
    }
}
