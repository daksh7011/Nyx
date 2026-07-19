package com.slothiesmooth.nyx.feature.decrypt.basic.presentation

import com.slothiesmooth.nyx.feature.decrypt.basic.domain.DecryptOutcome

private const val WRONG_PASSWORD = "Wrong password, or this image has been tampered with."
private const val NO_MESSAGE = "No hidden message was found in this image."

/** Render-ready reveal result: exactly one of [plaintext] / [error] is non-null. */
data class DecryptDisplay(val plaintext: String?, val error: String?)

/** Maps a [DecryptOutcome] to the honest, precise text the reveal screen shows. */
fun mapDecryptOutcome(outcome: DecryptOutcome): DecryptDisplay = when (outcome) {
    is DecryptOutcome.Success -> DecryptDisplay(plaintext = outcome.plaintext, error = null)
    DecryptOutcome.WrongPasswordOrTampered -> DecryptDisplay(plaintext = null, error = WRONG_PASSWORD)
    DecryptOutcome.NoHiddenMessage -> DecryptDisplay(plaintext = null, error = NO_MESSAGE)
    is DecryptOutcome.Failure -> DecryptDisplay(plaintext = null, error = outcome.reason)
}
