package com.slothiesmooth.nyx.feature.decrypt.basic.presentation

import com.slothiesmooth.nyx.feature.decrypt.basic.domain.DecryptOutcome
import com.slothiesmooth.nyx.feature.decrypt.basic.resources.Res
import com.slothiesmooth.nyx.feature.decrypt.basic.resources.decrypt_unreadable
import com.slothiesmooth.nyx.shared.presentation.text.UiText
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
    fun `failure maps to the generic unreadable message ignoring the technical reason`() {
        val display = mapDecryptOutcome(DecryptOutcome.Failure("codec error 0x5"))
        assertEquals(UiText.res(Res.string.decrypt_unreadable), display.error)
        assertNull(display.plaintext)
    }
}
