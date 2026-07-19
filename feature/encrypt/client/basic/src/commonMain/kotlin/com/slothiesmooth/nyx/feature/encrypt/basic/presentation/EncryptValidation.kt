package com.slothiesmooth.nyx.feature.encrypt.basic.presentation

import com.slothiesmooth.nyx.feature.encrypt.basic.resources.Res
import com.slothiesmooth.nyx.feature.encrypt.basic.resources.encrypt_passwords_do_not_match
import com.slothiesmooth.nyx.shared.presentation.text.UiText

/** Whether the compose step can encrypt yet, plus the inline error to surface (null while clean). */
data class EncryptValidation(val canEncrypt: Boolean, val error: UiText?)

/**
 * Pure validation for the message/password step. A mismatch is only surfaced once the user has begun
 * typing the confirmation, so an empty confirmation reads as "not done yet", not "wrong".
 */
fun validateEncryptInput(message: String, password: String, confirmPassword: String): EncryptValidation {
    val mismatch = confirmPassword.isNotEmpty() && password != confirmPassword
    val error = if (mismatch) UiText.res(Res.string.encrypt_passwords_do_not_match) else null
    val canEncrypt = message.isNotBlank() && password.isNotEmpty() && password == confirmPassword
    return EncryptValidation(canEncrypt = canEncrypt, error = error)
}
