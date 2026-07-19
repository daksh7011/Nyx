package com.slothiesmooth.nyx.feature.decrypt.basic.presentation

import com.slothiesmooth.nyx.feature.decrypt.basic.domain.DecryptOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class DecryptDisplayTest {

    @Test
    fun `success exposes plaintext and no error`() {
        val display = mapDecryptOutcome(DecryptOutcome.Success("hello"))
        assertEquals("hello", display.plaintext)
        assertNull(display.error)
    }

    @Test
    fun `wrong password and no-hidden-message render distinct honest errors`() {
        val wrong = mapDecryptOutcome(DecryptOutcome.WrongPasswordOrTampered)
        val none = mapDecryptOutcome(DecryptOutcome.NoHiddenMessage)
        assertNull(wrong.plaintext)
        assertNull(none.plaintext)
        assertNotEquals(wrong.error, none.error)
    }

    @Test
    fun `failure surfaces its reason`() {
        val display = mapDecryptOutcome(DecryptOutcome.Failure("This image could not be read."))
        assertEquals("This image could not be read.", display.error)
    }
}
