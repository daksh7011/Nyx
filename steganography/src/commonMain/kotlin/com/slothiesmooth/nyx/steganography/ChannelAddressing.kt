package com.slothiesmooth.nyx.steganography

internal const val BITS_PER_CHANNEL = 2
internal const val BITS_PER_BYTE = 8
internal const val CHANNELS_PER_PIXEL = 3
internal const val CHUNKS_PER_BYTE = BITS_PER_BYTE / BITS_PER_CHANNEL
internal const val CHUNK_MASK = 0x3
internal const val CHANNEL_MASK = 0xFF
private const val CLEAR_LOW_TWO_BITS = 0xFC
private const val RED_SLOT = 0
private const val GREEN_SLOT = 1
private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8
private const val BLUE_SHIFT = 0

/** The physical location (image, pixel, R/G/B slot) that a global channel index addresses. */
internal data class ChannelLocation(val imageIndex: Int, val pixelIndex: Int, val colorSlot: Int)

/**
 * Resolves a global channel index (spanning all [images], in input order) to the concrete
 * image/pixel/R-G-B-slot it addresses. Shared by the encode and decode paths so a given global
 * channel always names the same physical bit, regardless of write/read order.
 */
internal fun locateChannel(images: List<PixelImage>, globalChannel: Long): ChannelLocation {
    var remaining = globalChannel
    for ((imageIndex, image) in images.withIndex()) {
        val channels = image.width.toLong() * image.height.toLong() * CHANNELS_PER_PIXEL
        if (remaining < channels) {
            val pixelIndex = (remaining / CHANNELS_PER_PIXEL).toInt()
            val colorSlot = (remaining % CHANNELS_PER_PIXEL).toInt()
            return ChannelLocation(imageIndex, pixelIndex, colorSlot)
        }
        remaining -= channels
    }
    error("global channel $globalChannel is out of range for the supplied images")
}

/** Writes a 2-bit [chunk] to the pixel/slot that [globalChannel] resolves to, in [pixelArrays]. */
internal fun writeChunkAtGlobalChannel(
    images: List<PixelImage>,
    pixelArrays: List<IntArray>,
    globalChannel: Long,
    chunk: Int,
) {
    val location = locateChannel(images, globalChannel)
    val pixels = pixelArrays[location.imageIndex]
    pixels[location.pixelIndex] = writeChunk(pixels[location.pixelIndex], location.colorSlot, chunk)
}

/** Reads the 2-bit chunk from the pixel/slot that [globalChannel] resolves to, in [images]. */
internal fun readChunkAtGlobalChannel(images: List<PixelImage>, globalChannel: Long): Int {
    val location = locateChannel(images, globalChannel)
    return readChunk(images[location.imageIndex].pixels[location.pixelIndex], location.colorSlot)
}

private fun writeChunk(pixel: Int, colorSlot: Int, chunk: Int): Int {
    val shift = channelShift(colorSlot)
    val channelValue = (pixel ushr shift) and CHANNEL_MASK
    val updatedChannel = (channelValue and CLEAR_LOW_TWO_BITS) or chunk
    val clearMask = (CHANNEL_MASK shl shift).inv()
    return (pixel and clearMask) or (updatedChannel shl shift)
}

private fun readChunk(pixel: Int, colorSlot: Int): Int =
    (pixel ushr channelShift(colorSlot)) and CHUNK_MASK

private fun channelShift(colorSlot: Int): Int = when (colorSlot) {
    RED_SLOT -> RED_SHIFT
    GREEN_SLOT -> GREEN_SHIFT
    else -> BLUE_SHIFT
}
