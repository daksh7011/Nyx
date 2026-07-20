package com.slothiesmooth.nyx.feature.encrypt.basic.domain.usecase

import com.slothiesmooth.nyx.crypto.NyxCrypto
import com.slothiesmooth.nyx.feature.encrypt.basic.domain.EncryptOutcome
import com.slothiesmooth.nyx.shared.data.result.AppResult
import com.slothiesmooth.nyx.shared.data.source.ImageCodec
import com.slothiesmooth.nyx.steganography.Steganography
import com.slothiesmooth.nyx.steganography.StegoEncodeResult

private const val BITS_PER_BYTE = 8

/**
 * Decodes [coverBytes], encrypts [message] with [password], hides the encrypted blob in the cover
 * via LSB steganography, and re-encodes the result as PNG bytes. Reports a typed [EncryptOutcome];
 * a capacity shortfall carries approximate character counts for the UI to format, so no user-facing
 * text is built here.
 */
class EncryptMessageUseCase(
    private val crypto: NyxCrypto,
    private val stego: Steganography,
    private val codec: ImageCodec,
) {
    suspend operator fun invoke(coverBytes: ByteArray, message: String, password: String): EncryptOutcome {
        val pixelImage = when (val decoded = codec.decode(coverBytes)) {
            is AppResult.Ok -> decoded.value
            is AppResult.Err -> return EncryptOutcome.Failed
        }
        val blob = crypto.encrypt(message, password)
        return when (val encoded = stego.encode(listOf(pixelImage), blob)) {
            is StegoEncodeResult.Success -> when (val png = codec.encodePng(encoded.images.first())) {
                is AppResult.Ok -> EncryptOutcome.Success(png.value)
                is AppResult.Err -> EncryptOutcome.Failed
            }
            is StegoEncodeResult.CapacityExceeded -> EncryptOutcome.TooLarge(
                requiredChars = (encoded.requiredBits / BITS_PER_BYTE).toInt(),
                availableChars = (encoded.availableBits / BITS_PER_BYTE).toInt(),
            )
        }
    }
}
