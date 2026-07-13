package com.slothiesmooth.nyx.crypto

import kotlinx.coroutines.test.runTest
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
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
}
