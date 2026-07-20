package com.slothiesmooth.nyx.crypto

import dev.whyoleg.cryptography.BinarySize.Companion.bits
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.AES
import dev.whyoleg.cryptography.algorithms.PBKDF2
import dev.whyoleg.cryptography.algorithms.SHA256
import dev.whyoleg.cryptography.random.CryptographyRandom
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext
import kotlin.io.encoding.Base64

/**
 * AES-256-GCM authenticated encryption with a PBKDF2-HMAC-SHA256 (600k) derived key.
 *
 * Blob = `Base64(salt(16) || cipher.encrypt(plaintext))`, where the library's default
 * `cipher.encrypt` prepends a fresh 12-byte IV: `iv(12) || ciphertext || tag(16)`.
 */
class DefaultNyxCrypto(
    private val provider: CryptographyProvider = CryptographyProvider.Default,
) : NyxCrypto {

    override suspend fun encrypt(plaintext: String, password: String): String {
        // Empty passwords are meaningless for a secrecy tool and crash the Android PBKDF2 provider
        // (JVM accepts them) — reject up front so behavior is identical on every target.
        require(password.isNotEmpty()) { REASON_EMPTY_PASSWORD }
        val salt = CryptographyRandom.nextBytes(SALT_SIZE_BYTES)
        val cipherOutput = cipherFor(password, salt).encrypt(plaintext.encodeToByteArray())
        return Base64.Default.encode(salt + cipherOutput)
    }

    override suspend fun decrypt(blob: String, password: String): DecryptResult {
        if (password.isEmpty()) return DecryptResult.Failure(REASON_EMPTY_PASSWORD)
        val raw = decodeBlob(blob)
        return if (raw == null || raw.size < MIN_BLOB_SIZE_BYTES) {
            DecryptResult.Failure(failureReason(raw))
        } else {
            val salt = raw.copyOfRange(0, SALT_SIZE_BYTES)
            val cipherOutput = raw.copyOfRange(SALT_SIZE_BYTES, raw.size)
            runDecrypt(password, salt, cipherOutput)
        }
    }

    private suspend fun runDecrypt(
        password: String,
        salt: ByteArray,
        cipherOutput: ByteArray,
    ): DecryptResult {
        val cipher = cipherFor(password, salt)
        val attempt = runCatching { cipher.decrypt(cipherOutput).decodeToString() }
        // Re-throw if the coroutine was cancelled during decrypt (runCatching also catches
        // CancellationException); any other failure is a wrong password or a tampered blob.
        coroutineContext.ensureActive()
        return attempt.fold(
            onSuccess = { DecryptResult.Success(it) },
            onFailure = { DecryptResult.WrongPasswordOrTampered },
        )
    }

    private suspend fun cipherFor(password: String, salt: ByteArray) =
        provider.get(AES.GCM)
            .keyDecoder()
            .decodeFromByteArray(AES.Key.Format.RAW, derivedKeyBytes(password, salt))
            .cipher()

    private suspend fun derivedKeyBytes(password: String, salt: ByteArray): ByteArray =
        provider.get(PBKDF2)
            .secretDerivation(
                digest = SHA256,
                iterations = PBKDF2_ITERATIONS,
                outputSize = KEY_SIZE_BITS.bits,
                salt = salt,
            )
            .deriveSecretToByteArray(password.encodeToByteArray())

    private fun decodeBlob(blob: String): ByteArray? =
        runCatching { Base64.Default.decode(blob) }.getOrNull()

    private fun failureReason(raw: ByteArray?): String =
        if (raw == null) REASON_MALFORMED_BASE64 else REASON_TOO_SHORT
}
