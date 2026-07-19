package com.slothiesmooth.nyx.shared.testsupport

import com.slothiesmooth.nyx.shared.data.result.AppResult
import com.slothiesmooth.nyx.shared.data.source.ImageCodec
import com.slothiesmooth.nyx.steganography.PixelImage

private const val HEADER_INTS = 2
private const val BYTES_PER_INT = 4
private const val OPAQUE_ALPHA = 0xFF shl 24
private const val BYTE_MASK = 0xFF

/**
 * Lossless pixel round-trip so real stego survives in `commonTest`: `encodePng` packs
 * `[width][height][pixels…]` little-endian, `decode` unpacks and forces alpha opaque (the real
 * [ImageCodec] contract). Not a real PNG — a deterministic in-memory stand-in.
 */
class FakeImageCodec : ImageCodec {

    override suspend fun decode(bytes: ByteArray): AppResult<PixelImage> {
        val ints = IntArray(bytes.size / BYTES_PER_INT) { index -> readInt(bytes, index * BYTES_PER_INT) }
        val width = ints[0]
        val height = ints[1]
        val pixels = IntArray(width * height) { index -> ints[HEADER_INTS + index] or OPAQUE_ALPHA }
        return AppResult.Ok(PixelImage(width = width, height = height, pixels = pixels))
    }

    override suspend fun encodePng(image: PixelImage): AppResult<ByteArray> {
        val ints = IntArray(HEADER_INTS + image.pixels.size)
        ints[0] = image.width
        ints[1] = image.height
        image.pixels.copyInto(ints, destinationOffset = HEADER_INTS)
        val out = ByteArray(ints.size * BYTES_PER_INT)
        ints.forEachIndexed { index, value -> writeInt(out, index * BYTES_PER_INT, value) }
        return AppResult.Ok(out)
    }

    private fun readInt(bytes: ByteArray, offset: Int): Int {
        var result = 0
        for (byteIndex in 0 until BYTES_PER_INT) {
            result = result or ((bytes[offset + byteIndex].toInt() and BYTE_MASK) shl (byteIndex * Byte.SIZE_BITS))
        }
        return result
    }

    private fun writeInt(bytes: ByteArray, offset: Int, value: Int) {
        for (byteIndex in 0 until BYTES_PER_INT) {
            bytes[offset + byteIndex] = ((value shr (byteIndex * Byte.SIZE_BITS)) and BYTE_MASK).toByte()
        }
    }
}
