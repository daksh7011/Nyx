package com.slothiesmooth.nyx.feature.decrypt.basic.domain

/** Result of [com.slothiesmooth.nyx.feature.decrypt.basic.domain.usecase.DecryptMessageUseCase]. */
sealed interface DecryptOutcome {
    data class Success(val plaintext: String) : DecryptOutcome
    data object NoHiddenMessage : DecryptOutcome
    data object WrongPasswordOrTampered : DecryptOutcome
    data class Failure(val reason: String) : DecryptOutcome
}
