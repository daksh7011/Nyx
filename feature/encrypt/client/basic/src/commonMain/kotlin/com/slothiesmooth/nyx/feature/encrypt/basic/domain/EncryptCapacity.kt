package com.slothiesmooth.nyx.feature.encrypt.basic.domain

private const val USABLE_BITS_PER_PIXEL = 6 // 2 LSBs x R,G,B (mirrors :steganography's scheme)
private const val BITS_PER_BYTE = 8
private const val MARKER_OVERHEAD_BYTES = 6 // start + end 3-char markers
private const val CRYPTO_OVERHEAD_BYTES = 44 // salt(16) + iv(12) + tag(16)
private const val BASE64_NUMERATOR = 3
private const val BASE64_DENOMINATOR = 4

/**
 * Advisory upper bound on plaintext characters that fit in a [width] x [height] cover.
 *
 * The real encode path ([com.slothiesmooth.nyx.steganography.Steganography.encode]) is
 * authoritative; this mirrors its 2-bit-LSB-across-RGB capacity so the UI can warn before a user
 * types a message that won't fit.
 */
fun estimateMaxMessageChars(width: Int, height: Int): Int {
    val capacityBytes = width.toLong() * height.toLong() * USABLE_BITS_PER_PIXEL / BITS_PER_BYTE
    val payloadBytes = capacityBytes - MARKER_OVERHEAD_BYTES
    val encryptedBytes = payloadBytes * BASE64_NUMERATOR / BASE64_DENOMINATOR
    val messageBytes = encryptedBytes - CRYPTO_OVERHEAD_BYTES
    return messageBytes.coerceAtLeast(0L).toInt()
}
