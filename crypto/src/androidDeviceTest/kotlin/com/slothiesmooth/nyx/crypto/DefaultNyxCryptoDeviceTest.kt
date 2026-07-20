package com.slothiesmooth.nyx.crypto

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Proves the real Android crypto provider (BouncyCastle AES-GCM + PBKDF2) round-trips on device, and
 * that an empty password is rejected rather than crashing the provider — the exact divergence that
 * only surfaced on a real device (host tests use the JDK provider).
 */
@RunWith(AndroidJUnit4::class)
class DefaultNyxCryptoDeviceTest {

    @Test
    fun roundTripsOnDeviceAndRejectsEmptyPassword() = runBlocking {
        val crypto = DefaultNyxCrypto()

        val blob = crypto.encrypt("attack at dawn", "correct horse")
        assertEquals(DecryptResult.Success("attack at dawn"), crypto.decrypt(blob, "correct horse"))

        // Empty password: was a device-only crash (BouncyCastle "password empty"); now consistently rejected.
        assertTrue(crypto.decrypt(blob, "") is DecryptResult.Failure)
        var threwOnEmptyEncrypt = false
        try {
            crypto.encrypt("secret", "")
        } catch (expected: IllegalArgumentException) {
            threwOnEmptyEncrypt = true
        }
        assertTrue("empty password must be rejected on device", threwOnEmptyEncrypt)
    }
}
