package com.slothiesmooth.nyx.steganography

/** Extracts the 2-bit chunk at [chunkIndex] from the marker-framed [frameBytes]. */
internal fun chunkAt(frameBytes: ByteArray, chunkIndex: Int): Int {
    val byteIndex = chunkIndex / CHUNKS_PER_BYTE
    val subIndex = chunkIndex % CHUNKS_PER_BYTE
    val bitOffset = (CHUNKS_PER_BYTE - 1 - subIndex) * BITS_PER_CHANNEL
    return ((frameBytes[byteIndex].toInt() and CHANNEL_MASK) ushr bitOffset) and CHUNK_MASK
}

/** True once enough bytes for [startBytes] have decoded but they don't match it: no payload present. */
internal fun markerBroken(decoded: List<Byte>, startBytes: ByteArray): Boolean =
    decoded.size == startBytes.size && !startsWithBytes(decoded, startBytes)

/** Returns the payload between [startBytes] and [endBytes] once [decoded] ends with the end marker. */
internal fun completedFrame(
    decoded: List<Byte>,
    startBytes: ByteArray,
    endBytes: ByteArray,
): ByteArray? {
    val minFrameSize = startBytes.size + endBytes.size
    return if (decoded.size >= minFrameSize && endsWithBytes(decoded, endBytes)) {
        decoded.subList(startBytes.size, decoded.size - endBytes.size).toByteArray()
    } else {
        null
    }
}

private fun startsWithBytes(data: List<Byte>, prefix: ByteArray): Boolean {
    if (data.size < prefix.size) return false
    return prefix.indices.all { data[it] == prefix[it] }
}

private fun endsWithBytes(data: List<Byte>, suffix: ByteArray): Boolean {
    if (data.size < suffix.size) return false
    val offset = data.size - suffix.size
    return suffix.indices.all { data[offset + it] == suffix[it] }
}
