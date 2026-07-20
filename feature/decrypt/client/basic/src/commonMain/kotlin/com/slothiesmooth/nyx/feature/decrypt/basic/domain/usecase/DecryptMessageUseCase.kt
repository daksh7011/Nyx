package com.slothiesmooth.nyx.feature.decrypt.basic.domain.usecase

import com.slothiesmooth.nyx.crypto.DecryptResult
import com.slothiesmooth.nyx.crypto.NyxCrypto
import com.slothiesmooth.nyx.feature.decrypt.basic.domain.DecryptOutcome
import com.slothiesmooth.nyx.shared.data.result.AppError
import com.slothiesmooth.nyx.shared.data.result.AppResult
import com.slothiesmooth.nyx.shared.data.source.ImageCodec
import com.slothiesmooth.nyx.steganography.Steganography

private const val UNREADABLE_IMAGE = "This image could not be read."

/**
 * Decodes [imageBytes], extracts a hidden blob via LSB steganography (if any), and decrypts it
 * with [password]. The three failure modes — unreadable image, no hidden payload, wrong password
 * or tampering — are distinguished so the UI can show a precise message.
 */
class DecryptMessageUseCase(
    private val crypto: NyxCrypto,
    private val stego: Steganography,
    private val codec: ImageCodec,
) {
    suspend operator fun invoke(imageBytes: ByteArray, password: String): DecryptOutcome {
        val pixelImage = when (val decoded = codec.decode(imageBytes)) {
            is AppResult.Ok -> decoded.value
            is AppResult.Err -> return DecryptOutcome.Failure(reasonFor(decoded.cause))
        }
        val blob = stego.decode(listOf(pixelImage)) ?: return DecryptOutcome.NoHiddenMessage
        return when (val result = crypto.decrypt(blob, password)) {
            is DecryptResult.Success -> DecryptOutcome.Success(result.plaintext)
            DecryptResult.WrongPasswordOrTampered -> DecryptOutcome.WrongPasswordOrTampered
            is DecryptResult.Failure -> DecryptOutcome.Failure(result.reason)
        }
    }

    private fun reasonFor(error: AppError): String = when (error) {
        is AppError.Storage -> error.message
        is AppError.Validation -> error.message
        else -> UNREADABLE_IMAGE
    }
}
