package com.slothiesmooth.nyx.steganography

import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

/**
 * Hides a UTF-8 payload in the low 2 bits of each R/G/B channel (6 bits per pixel), framed with
 * [startMarker] / [endMarker], optionally spanning multiple covers. Alpha is never touched.
 *
 * Payload chunks are written to a Feistel-permuted channel order (see [ChannelPermutation]) so
 * the embedding is scattered across the whole image rather than packed into the first channels.
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
        val permutation = ChannelPermutation(channelCount(images))
        val pixelArrays = images.map { it.pixels.copyOf() }
        for (chunkIndex in 0 until totalChunks) {
            val globalChannel = permutation.map(chunkIndex.toLong())
            writeChunkAtGlobalChannel(images, pixelArrays, globalChannel, chunkAt(frameBytes, chunkIndex))
        }
        return images.mapIndexed { index, image -> PixelImage(image.width, image.height, pixelArrays[index]) }
    }

    suspend fun decode(images: List<PixelImage>): String? {
        coroutineContext.ensureActive()
        return extractFrameBytes(images)?.decodeToString()
    }

    private fun extractFrameBytes(images: List<PixelImage>): ByteArray? {
        val startBytes = startMarker.encodeToByteArray()
        val endBytes = endMarker.encodeToByteArray()
        val decoded = ArrayList<Byte>()
        val totalChannels = channelCount(images)
        val permutation = ChannelPermutation(totalChannels)
        var partialByte = 0
        var chunkCount = 0
        var chunkIndex = 0L
        var frame: ByteArray? = null
        while (chunkIndex < totalChannels && frame == null) {
            val globalChannel = permutation.map(chunkIndex)
            partialByte = (partialByte shl BITS_PER_CHANNEL) or readChunkAtGlobalChannel(images, globalChannel)
            chunkCount++
            if (chunkCount == CHUNKS_PER_BYTE) {
                decoded.add(partialByte.toByte())
                partialByte = 0
                chunkCount = 0
                if (markerBroken(decoded, startBytes)) return null
                frame = completedFrame(decoded, startBytes, endBytes)
            }
            chunkIndex++
        }
        return frame
    }
}
