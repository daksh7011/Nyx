package com.slothiesmooth.nyx.crypto

/**
 * Authenticated encryption for Nyx. Produces and consumes a self-describing Base64 blob that the
 * steganography engine hides inside an image.
 *
 * Both operations are `suspend` because the wasmJs provider (WebCrypto) is asynchronous.
 */
interface NyxCrypto {

    /** Returns `Base64(salt(16) || iv(12) || ciphertext || tag(16))`. */
    suspend fun encrypt(plaintext: String, password: String): String

    suspend fun decrypt(blob: String, password: String): DecryptResult
}
