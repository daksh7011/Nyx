package com.slothiesmooth.nyx.feature.encrypt.basic.presentation

import com.slothiesmooth.nyx.feature.encrypt.basic.resources.Res
import com.slothiesmooth.nyx.feature.encrypt.basic.resources.encrypt_passwords_do_not_match
import com.slothiesmooth.nyx.shared.presentation.text.UiText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EncryptValidationTest {

    @Test
    fun `blank message cannot encrypt and shows no error yet`() {
        val result = validateEncryptInput(message = "", password = "pw", confirmPassword = "pw")
        assertFalse(result.canEncrypt)
        assertNull(result.error)
    }

    @Test
    fun `mismatched passwords surface an error and block encrypt`() {
        val result = validateEncryptInput(message = "hi", password = "pw", confirmPassword = "px")
        assertFalse(result.canEncrypt)
        assertEquals(UiText.res(Res.string.encrypt_passwords_do_not_match), result.error)
    }

    @Test
    fun `a half-typed confirmation is not yet a mismatch error`() {
        val result = validateEncryptInput(message = "hi", password = "pw", confirmPassword = "")
        assertFalse(result.canEncrypt)
        assertNull(result.error)
    }

    @Test
    fun `matching passwords with a message allow encrypt`() {
        val result = validateEncryptInput(message = "hi", password = "pw", confirmPassword = "pw")
        assertTrue(result.canEncrypt)
        assertNull(result.error)
    }
}
