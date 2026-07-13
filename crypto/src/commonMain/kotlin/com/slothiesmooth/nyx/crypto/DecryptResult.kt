package com.slothiesmooth.nyx.crypto

/** Result of [NyxCrypto.decrypt]. */
sealed interface DecryptResult {

    /** The blob decrypted and authenticated; [plaintext] is the recovered message. */
    data class Success(val plaintext: String) : DecryptResult

    /** The GCM tag failed: wrong password, or the blob was altered after encryption. */
    data object WrongPasswordOrTampered : DecryptResult

    /** The blob could not be parsed (bad Base64, or too short to contain salt + nonce + tag). */
    data class Failure(val reason: String) : DecryptResult
}
