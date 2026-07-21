package com.slothiesmooth.nyx.feature.encrypt.basic.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import com.slothiesmooth.nyx.shared.presentation.state.MutableViewState
import com.slothiesmooth.nyx.shared.presentation.state.ViewState
import com.slothiesmooth.nyx.shared.presentation.text.UiText

/** The four wizard steps: pick a cover image, write the message, set the password, then the result. */
enum class EncryptStep { PickImage, Message, Password, Result }

/**
 * Read-only wizard state the screen observes. Everything is render-ready: the [thumbnail] is already
 * decoded, [maxCharsLabel] is already formatted, and [messageReady]/[canEncrypt]/[validationError]
 * are already computed by the view model — the composables never map, format, or validate.
 */
interface EncryptState : ViewState {
    val step: EncryptStep
    val thumbnail: ImageBitmap?
    val hasImage: Boolean
    val maxCharsLabel: UiText?
    val message: String
    val messageReady: Boolean
    val password: String
    val confirmPassword: String
    val validationError: UiText?
    val canEncrypt: Boolean
    val savedName: String?
    val showCamera: Boolean
}

/** Mutable backing state owned by [EncryptViewModel] and reused by the content previews. */
class EncryptMutableState(cameraVisible: Boolean) : MutableViewState(), EncryptState {
    override var step: EncryptStep by mutableStateOf(EncryptStep.PickImage)
    override var thumbnail: ImageBitmap? by mutableStateOf(null)
    override var maxCharsLabel: UiText? by mutableStateOf(null)
    override var message: String by mutableStateOf("")
    override var password: String by mutableStateOf("")
    override var confirmPassword: String by mutableStateOf("")
    override var validationError: UiText? by mutableStateOf(null)
    override var canEncrypt: Boolean by mutableStateOf(false)
    override var savedName: String? by mutableStateOf(null)
    override val showCamera: Boolean = cameraVisible
    override val hasImage: Boolean get() = thumbnail != null
    override val messageReady: Boolean get() = message.isNotBlank()
}
