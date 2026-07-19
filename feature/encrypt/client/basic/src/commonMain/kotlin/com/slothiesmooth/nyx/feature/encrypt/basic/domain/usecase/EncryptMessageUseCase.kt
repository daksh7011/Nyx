package com.slothiesmooth.nyx.feature.encrypt.basic.domain.usecase

import com.slothiesmooth.nyx.crypto.NyxCrypto
import com.slothiesmooth.nyx.shared.data.result.AppError
import com.slothiesmooth.nyx.shared.data.result.AppResult
import com.slothiesmooth.nyx.shared.data.source.ImageCodec
import com.slothiesmooth.nyx.steganography.Steganography
import com.slothiesmooth.nyx.steganography.StegoEncodeResult

private const val BITS_PER_BYTE = 8

/**
 * Decodes [coverBytes], encrypts [message] with [password], hides the encrypted blob in the cover
 * via LSB steganography, and re-encodes the result as PNG bytes ready to save or share.
 */
class EncryptMessageUseCase(
    private val crypto: NyxCrypto,
    private val stego: Steganography,
    private val codec: ImageCodec,
) {
    suspend operator fun invoke(coverBytes: ByteArray, message: String, password: String): AppResult<ByteArray> {
        val pixelImage = when (val decoded = codec.decode(coverBytes)) {
            is AppResult.Ok -> decoded.value
            is AppResult.Err -> return decoded
        }
        val blob = crypto.encrypt(message, password)
        return when (val encoded = stego.encode(listOf(pixelImage), blob)) {
            is StegoEncodeResult.Success -> codec.encodePng(encoded.images.first())
            is StegoEncodeResult.CapacityExceeded -> AppResult.Err(AppError.Validation(capacityMessage(encoded)))
        }
    }

    private fun capacityMessage(result: StegoEncodeResult.CapacityExceeded): String {
        val required = result.requiredBits / BITS_PER_BYTE
        val available = result.availableBits / BITS_PER_BYTE
        return "Message too large for this image. It needs about $required characters of capacity " +
            "but this image holds about $available. Use a larger image or a shorter message."
    }
}
