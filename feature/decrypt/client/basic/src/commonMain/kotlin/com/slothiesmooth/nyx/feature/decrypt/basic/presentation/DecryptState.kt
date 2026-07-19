package com.slothiesmooth.nyx.feature.decrypt.basic.presentation

import androidx.compose.ui.graphics.ImageBitmap
import com.slothiesmooth.nyx.shared.presentation.state.ViewState

/**
 * Read-only reveal state the screen observes. Everything is render-ready: [thumbnail] is already
 * decoded, [canDecrypt] is already computed, and [plaintext]/[errorMessage] already carry the
 * honest reveal outcome — the composables never decode, validate, or map.
 */
interface DecryptState : ViewState {
    val thumbnail: ImageBitmap?
    val hasImage: Boolean
    val isFromVault: Boolean
    val password: String
    val canDecrypt: Boolean
    val plaintext: String?
    val errorMessage: String?
}
