package com.slothiesmooth.nyx.feature.decrypt.basic.presentation

import com.slothiesmooth.nyx.feature.decrypt.basic.domain.DecryptOutcome
import com.slothiesmooth.nyx.feature.decrypt.basic.resources.Res
import com.slothiesmooth.nyx.feature.decrypt.basic.resources.decrypt_no_message
import com.slothiesmooth.nyx.feature.decrypt.basic.resources.decrypt_unreadable
import com.slothiesmooth.nyx.feature.decrypt.basic.resources.decrypt_wrong_password
import com.slothiesmooth.nyx.shared.presentation.text.UiText

/** Render-ready reveal result: exactly one of [plaintext] / [error] is non-null. */
data class DecryptDisplay(val plaintext: String?, val error: UiText?)

/**
 * Maps a [DecryptOutcome] to the honest text the reveal screen shows. A [DecryptOutcome.Failure]
 * carries a technical reason for logging; the UI shows a single generic "unreadable" message.
 */
fun mapDecryptOutcome(outcome: DecryptOutcome): DecryptDisplay = when (outcome) {
    is DecryptOutcome.Success -> DecryptDisplay(plaintext = outcome.plaintext, error = null)
    DecryptOutcome.WrongPasswordOrTampered ->
        DecryptDisplay(plaintext = null, error = UiText.res(Res.string.decrypt_wrong_password))
    DecryptOutcome.NoHiddenMessage ->
        DecryptDisplay(plaintext = null, error = UiText.res(Res.string.decrypt_no_message))
    is DecryptOutcome.Failure ->
        DecryptDisplay(plaintext = null, error = UiText.res(Res.string.decrypt_unreadable))
}
