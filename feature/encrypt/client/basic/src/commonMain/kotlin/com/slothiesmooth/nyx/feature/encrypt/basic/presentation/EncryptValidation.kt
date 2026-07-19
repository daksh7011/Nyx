package com.slothiesmooth.nyx.feature.encrypt.basic.presentation

private const val PASSWORDS_DO_NOT_MATCH = "Passwords do not match"

/** Whether the compose step can encrypt yet, plus the inline error to surface (null while clean). */
data class EncryptValidation(val canEncrypt: Boolean, val error: String?)

/**
 * Pure validation for the message/password step. A mismatch is only surfaced once the user has begun
 * typing the confirmation, so an empty confirmation reads as "not done yet", not "wrong".
 */
fun validateEncryptInput(message: String, password: String, confirmPassword: String): EncryptValidation {
    val mismatch = confirmPassword.isNotEmpty() && password != confirmPassword
    val error = if (mismatch) PASSWORDS_DO_NOT_MATCH else null
    val canEncrypt = message.isNotBlank() && password.isNotEmpty() && password == confirmPassword
    return EncryptValidation(canEncrypt = canEncrypt, error = error)
}
