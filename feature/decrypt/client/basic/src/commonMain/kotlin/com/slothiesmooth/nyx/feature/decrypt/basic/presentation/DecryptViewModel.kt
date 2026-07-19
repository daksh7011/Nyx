package com.slothiesmooth.nyx.feature.decrypt.basic.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import com.slothiesmooth.nyx.feature.decrypt.basic.domain.usecase.DecryptMessageUseCase
import com.slothiesmooth.nyx.feature.decrypt.basic.domain.usecase.LoadVaultImageBytesUseCase
import com.slothiesmooth.nyx.shared.data.id.StegoImageId
import com.slothiesmooth.nyx.shared.data.result.AppResult
import com.slothiesmooth.nyx.shared.presentation.image.toImageBitmap
import com.slothiesmooth.nyx.shared.presentation.state.MutableViewState
import com.slothiesmooth.nyx.shared.presentation.state.UiState
import com.slothiesmooth.nyx.shared.presentation.viewmodel.BaseViewModel

private const val LOAD_FAILED = "This image could not be loaded from the vault."

/** Mutable backing state owned by [DecryptViewModel]. */
private class DecryptMutableState : MutableViewState(), DecryptState {
    override var thumbnail: ImageBitmap? by mutableStateOf(null)
    override var hasImage: Boolean by mutableStateOf(false)
    override var isFromVault: Boolean by mutableStateOf(false)
    override var password: String by mutableStateOf("")
    override var canDecrypt: Boolean by mutableStateOf(false)
    override var plaintext: String? by mutableStateOf(null)
    override var errorMessage: String? by mutableStateOf(null)
}

/**
 * Drives the reveal flow: loads vault bytes (or accepts a picked image), decodes the thumbnail,
 * gates decryption on a non-empty password, runs the decrypt use case, and maps its [DecryptOutcome]
 * to render-ready plaintext/error via [mapDecryptOutcome]. All decoding, gating, and error mapping
 * live here so the composables only observe [DecryptState].
 */
class DecryptViewModel(
    private val decryptMessage: DecryptMessageUseCase,
    private val loadVaultImageBytes: LoadVaultImageBytesUseCase,
) : BaseViewModel() {

    private val mutableState = DecryptMutableState()
    val state: DecryptState get() = mutableState
    private var imageBytes: ByteArray? = null

    fun load(imageId: String?) {
        if (imageId == null) return
        async("load", force = true) {
            when (val result = loadVaultImageBytes(StegoImageId(imageId))) {
                is AppResult.Ok -> setImage(result.value, fromVault = true)
                is AppResult.Err -> withState {
                    mutableState.isFromVault = true
                    mutableState.errorMessage = LOAD_FAILED
                }
            }
        }
    }

    fun onImagePicked(bytes: ByteArray) {
        async("pick") { setImage(bytes, fromVault = false) }
    }

    private suspend fun setImage(bytes: ByteArray, fromVault: Boolean) {
        imageBytes = bytes
        val thumb = runCatching { bytes.toImageBitmap() }.getOrNull()
        withState {
            mutableState.thumbnail = thumb
            mutableState.hasImage = true
            mutableState.isFromVault = fromVault
            mutableState.plaintext = null
            mutableState.errorMessage = null
            recomputeCanDecrypt()
        }
    }

    fun onPasswordChange(value: String) = withState {
        mutableState.password = value
        recomputeCanDecrypt()
    }

    fun decrypt() {
        val bytes = imageBytes ?: return
        if (!mutableState.canDecrypt) return
        withState { mutableState.uiState = UiState.Blocking }
        async("decrypt") {
            val display = mapDecryptOutcome(decryptMessage(bytes, mutableState.password))
            withState {
                mutableState.plaintext = display.plaintext
                mutableState.errorMessage = display.error
                mutableState.uiState = UiState.Ready
            }
        }
    }

    private fun recomputeCanDecrypt() {
        mutableState.canDecrypt = imageBytes != null && mutableState.password.isNotEmpty()
    }
}
