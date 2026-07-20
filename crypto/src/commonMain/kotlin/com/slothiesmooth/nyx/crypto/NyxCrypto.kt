package com.slothiesmooth.nyx.crypto

/**
 * Authenticated encryption for Nyx. Produces and consumes a self-describing Base64 blob that the
 * steganography engine hides inside an image.
 *
 * Both operations are `suspend` because the wasmJs provider (WebCrypto) is asynchronous.
 */
interface NyxCrypto {

    /**
     * Returns `Base64(salt(16) || iv(12) || ciphertext || tag(16))`.
     *
     * @throws IllegalArgumentException if [password] is empty — rejected on every target for
     * consistency (the Android PBKDF2 provider rejects empty passwords, the JVM one does not).
     */
    suspend fun encrypt(plaintext: String, password: String): String

    /** An empty [password] yields [DecryptResult.Failure], mirroring [encrypt]'s rejection. */
    suspend fun decrypt(blob: String, password: String): DecryptResult
}
