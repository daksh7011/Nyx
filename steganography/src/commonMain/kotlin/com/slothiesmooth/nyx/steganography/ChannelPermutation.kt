package com.slothiesmooth.nyx.steganography

/**
 * A deterministic, key-fixed bijection on the channel index range `[0, channelCount)`.
 * Used to scatter LSB writes across the whole image so the embedding leaves no clustered
 * region for LSB steganalysis (RS / sample-pair) to lock onto. O(1) memory: positions are
 * computed on demand, never stored. Identical on every KMP target (pure Long integer math).
 */
internal class ChannelPermutation(private val channelCount: Long) {
    // Smallest even bit-width whose domain covers channelCount, so the two Feistel halves are equal.
    private val halfBits: Int = run {
        var bits = 1
        while ((1L shl (2 * bits)) < channelCount) bits++
        bits
    }
    private val halfMask: Long = (1L shl halfBits) - 1L

    /** Maps index k in [0, channelCount) to a scattered channel in [0, channelCount), bijectively. */
    fun map(index: Long): Long {
        require(index in 0 until channelCount)
        var x = index
        do {
            x = feistel(x)
        } while (x >= channelCount) // cycle-walk: stay inside the real range
        return x
    }

    private fun feistel(value: Long): Long {
        var left = (value ushr halfBits) and halfMask
        var right = value and halfMask
        repeat(ROUNDS) { round ->
            val next = right
            right = left xor roundFunction(right, round)
            left = next
        }
        return (left shl halfBits) or right
    }

    // Any deterministic function keeps Feistel a bijection; this one mixes well and is cross-platform.
    private fun roundFunction(half: Long, round: Int): Long {
        var h = half
        h = (h + round.toLong() * ROUND_CONST) and halfMask
        h = (h * MIX_ODD_MULTIPLIER) and halfMask
        h = (h xor (h ushr MIX_SHIFT)) and halfMask
        return h
    }

    private companion object {
        const val ROUNDS = 4
        const val MIX_ODD_MULTIPLIER = 0x9E3779B1L // odd (golden-ratio) -> good bit diffusion
        const val ROUND_CONST = 0x7F4A7C15L
        const val MIX_SHIFT = 13
    }
}
