package com.slothiesmooth.nyx.shared.data.source

import com.slothiesmooth.nyx.shared.data.result.AppResult
import com.slothiesmooth.nyx.steganography.PixelImage

/**
 * Decodes encoded image bytes (JPEG/PNG) to a [PixelImage] and re-encodes to PNG (lossless out).
 * Contract: [decode] FORCES every pixel opaque (alpha = 0xFF) before returning — premultiplication
 * on Android/skiko round-trips corrupts the LSBs of alpha<255 pixels, which is fatal to LSB
 * steganography. Implementations live in `:client` (expect/actual: Android BitmapFactory /
 * skiko for the rest).
 */
interface ImageCodec {
    suspend fun decode(bytes: ByteArray): AppResult<PixelImage>
    suspend fun encodePng(image: PixelImage): AppResult<ByteArray>
}
