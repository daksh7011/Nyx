package com.slothiesmooth.nyx.crypto

import kotlinx.coroutines.test.runTest
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class DefaultNyxCryptoTest {

    @Test
    fun `round trips a plaintext with the correct password`() = runTest {
        val crypto = DefaultNyxCrypto()
        val blob = crypto.encrypt("attack at dawn", "correct horse")
        assertEquals(DecryptResult.Success("attack at dawn"), crypto.decrypt(blob, "correct horse"))
    }

    @Test
    fun `reports wrong password without decrypting`() = runTest {
        val crypto = DefaultNyxCrypto()
        val blob = crypto.encrypt("attack at dawn", "correct horse")
        assertEquals(DecryptResult.WrongPasswordOrTampered, crypto.decrypt(blob, "wrong horse"))
    }

    @Test
    fun `detects a single-byte tamper in the ciphertext region`() = runTest {
        val crypto = DefaultNyxCrypto()
        val blob = crypto.encrypt("attack at dawn", "correct horse")
        val raw = Base64.Default.decode(blob)
        // First byte past salt(16) + nonce(12): the ciphertext-or-tag region.
        val tamperIndex = SALT_SIZE_BYTES + NONCE_SIZE_BYTES
        raw[tamperIndex] = (raw[tamperIndex].toInt() xor 0xFF).toByte()
        val tampered = Base64.Default.encode(raw)
        assertEquals(DecryptResult.WrongPasswordOrTampered, crypto.decrypt(tampered, "correct horse"))
    }

    @Test
    fun `produces a different blob each time for the same input`() = runTest {
        val crypto = DefaultNyxCrypto()
        val first = crypto.encrypt("attack at dawn", "correct horse")
        val second = crypto.encrypt("attack at dawn", "correct horse")
        assertNotEquals(first, second)
        assertEquals(DecryptResult.Success("attack at dawn"), crypto.decrypt(first, "correct horse"))
        assertEquals(DecryptResult.Success("attack at dawn"), crypto.decrypt(second, "correct horse"))
    }

    @Test
    fun `blob decodes to at least salt plus nonce plus tag bytes`() = runTest {
        val crypto = DefaultNyxCrypto()
        val raw = Base64.Default.decode(crypto.encrypt("", "correct horse"))
        assertTrue(raw.size >= MIN_BLOB_SIZE_BYTES, "expected >= $MIN_BLOB_SIZE_BYTES, got ${raw.size}")
    }

    @Test
    fun `round trips an empty plaintext`() = runTest {
        val crypto = DefaultNyxCrypto()
        val blob = crypto.encrypt("", "correct horse")
        assertEquals(DecryptResult.Success(""), crypto.decrypt(blob, "correct horse"))
    }

    @Test
    fun `round trips a unicode plaintext`() = runTest {
        val crypto = DefaultNyxCrypto()
        val secret = "rendezvous 🦊🔒 07:30 — café"
        val blob = crypto.encrypt(secret, "correct horse")
        assertEquals(DecryptResult.Success(secret), crypto.decrypt(blob, "correct horse"))
    }

    @Test
    fun `rejects an empty password consistently across platforms`() = runTest {
        // Empty passwords are meaningless for a secrecy tool and crash the Android PBKDF2 provider
        // (they compiled/passed only on JVM) — now rejected identically on every target.
        val crypto = DefaultNyxCrypto()
        val blob = crypto.encrypt("secret", "correct horse")
        assertFailsWith<IllegalArgumentException> { crypto.encrypt("secret", "") }
        assertEquals(REASON_EMPTY_PASSWORD, assertIs<DecryptResult.Failure>(crypto.decrypt(blob, "")).reason)
    }

    @Test
    fun `fails cleanly on a non-Base64 blob`() = runTest {
        val crypto = DefaultNyxCrypto()
        val result = crypto.decrypt("this is not base64 @@@", "correct horse")
        assertEquals(REASON_MALFORMED_BASE64, assertIs<DecryptResult.Failure>(result).reason)
    }

    @Test
    fun `fails cleanly on a blob that is too short`() = runTest {
        val crypto = DefaultNyxCrypto()
        // Valid Base64 but only 10 bytes -> below the 44-byte minimum.
        val shortBlob = Base64.Default.encode(ByteArray(10))
        val result = crypto.decrypt(shortBlob, "correct horse")
        assertEquals(REASON_TOO_SHORT, assertIs<DecryptResult.Failure>(result).reason)
    }
}
