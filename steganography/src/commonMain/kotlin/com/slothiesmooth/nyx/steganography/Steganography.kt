package com.slothiesmooth.nyx.steganography

import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

private const val BITS_PER_CHANNEL = 2
private const val BITS_PER_BYTE = 8
private const val CHANNELS_PER_PIXEL = 3
private const val CHUNKS_PER_BYTE = 4
private const val CHUNK_MASK = 0x3
private const val CHANNEL_MASK = 0xFF
private const val CLEAR_LOW_TWO_BITS = 0xFC
private const val RED_SLOT = 0
private const val GREEN_SLOT = 1
private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8
private const val BLUE_SHIFT = 0

/**
 * Hides a UTF-8 payload in the low 2 bits of each R/G/B channel (6 bits per pixel), framed with
 * [startMarker] / [endMarker], optionally spanning multiple covers. Alpha is never touched.
 */
class Steganography(
    private val startMarker: String = "@!#",
    private val endMarker: String = "#!@",
) {

    suspend fun encode(images: List<PixelImage>, payload: String): StegoEncodeResult {
        coroutineContext.ensureActive()
        val frameBytes = buildFrame(payload)
        val requiredBits = frameBytes.size.toLong() * BITS_PER_BYTE
        val availableBits = channelCount(images) * BITS_PER_CHANNEL
        if (requiredBits > availableBits) {
            return StegoEncodeResult.CapacityExceeded(requiredBits, availableBits)
        }
        return StegoEncodeResult.Success(embed(images, frameBytes))
    }

    private fun buildFrame(payload: String): ByteArray =
        (startMarker + payload + endMarker).encodeToByteArray()

    private fun channelCount(images: List<PixelImage>): Long =
        images.sumOf { it.width.toLong() * it.height.toLong() * CHANNELS_PER_PIXEL }

    private fun embed(images: List<PixelImage>, frameBytes: ByteArray): List<PixelImage> {
        val totalChunks = frameBytes.size * CHUNKS_PER_BYTE
        var chunkIndex = 0
        return images.map { image ->
            val pixels = image.pixels.copyOf()
            val channels = image.width * image.height * CHANNELS_PER_PIXEL
            var localChannel = 0
            while (chunkIndex < totalChunks && localChannel < channels) {
                val pixelIndex = localChannel / CHANNELS_PER_PIXEL
                val colorSlot = localChannel % CHANNELS_PER_PIXEL
                pixels[pixelIndex] =
                    writeChunk(pixels[pixelIndex], colorSlot, chunkAt(frameBytes, chunkIndex))
                chunkIndex++
                localChannel++
            }
            PixelImage(image.width, image.height, pixels)
        }
    }

    private fun chunkAt(frameBytes: ByteArray, chunkIndex: Int): Int {
        val byteIndex = chunkIndex / CHUNKS_PER_BYTE
        val subIndex = chunkIndex % CHUNKS_PER_BYTE
        val bitOffset = (CHUNKS_PER_BYTE - 1 - subIndex) * BITS_PER_CHANNEL
        return ((frameBytes[byteIndex].toInt() and CHANNEL_MASK) ushr bitOffset) and CHUNK_MASK
    }

    private fun writeChunk(pixel: Int, colorSlot: Int, chunk: Int): Int {
        val shift = channelShift(colorSlot)
        val channelValue = (pixel ushr shift) and CHANNEL_MASK
        val updatedChannel = (channelValue and CLEAR_LOW_TWO_BITS) or chunk
        val clearMask = (CHANNEL_MASK shl shift).inv()
        return (pixel and clearMask) or (updatedChannel shl shift)
    }

    private fun channelShift(colorSlot: Int): Int = when (colorSlot) {
        RED_SLOT -> RED_SHIFT
        GREEN_SLOT -> GREEN_SHIFT
        else -> BLUE_SHIFT
    }
}
