package com.slothiesmooth.nyx.feature.encrypt.basic.domain

/** Outcome of [com.slothiesmooth.nyx.feature.encrypt.basic.domain.usecase.EncryptMessageUseCase]. */
sealed interface EncryptOutcome {

    /** The message was hidden; [pngBytes] is the stego PNG ready to save or share. */
    class Success(val pngBytes: ByteArray) : EncryptOutcome

    /**
     * The message needs more capacity than the cover offers. [requiredChars] and [availableChars]
     * are approximate character counts for the UI to format into a translatable message.
     */
    data class TooLarge(val requiredChars: Int, val availableChars: Int) : EncryptOutcome

    /** The cover could not be decoded or the stego result could not be re-encoded. */
    data object Failed : EncryptOutcome
}
