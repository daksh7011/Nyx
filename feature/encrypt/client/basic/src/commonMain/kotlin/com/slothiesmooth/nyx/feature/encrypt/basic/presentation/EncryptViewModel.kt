package com.slothiesmooth.nyx.feature.encrypt.basic.presentation

import com.slothiesmooth.nyx.feature.encrypt.basic.domain.EncryptOutcome
import com.slothiesmooth.nyx.feature.encrypt.basic.domain.estimateMaxMessageChars
import com.slothiesmooth.nyx.feature.encrypt.basic.domain.usecase.EncryptMessageUseCase
import com.slothiesmooth.nyx.feature.encrypt.basic.domain.usecase.SaveToVaultUseCase
import com.slothiesmooth.nyx.feature.encrypt.basic.resources.Res
import com.slothiesmooth.nyx.feature.encrypt.basic.resources.encrypt_capacity_hint
import com.slothiesmooth.nyx.feature.encrypt.basic.resources.encrypt_failed
import com.slothiesmooth.nyx.feature.encrypt.basic.resources.encrypt_too_large
import com.slothiesmooth.nyx.shared.data.result.AppResult
import com.slothiesmooth.nyx.shared.data.source.CameraSource
import com.slothiesmooth.nyx.shared.data.source.ImageCodec
import com.slothiesmooth.nyx.shared.data.source.ImagePicker
import com.slothiesmooth.nyx.shared.data.source.PlatformCapabilities
import com.slothiesmooth.nyx.shared.data.source.ShareSource
import com.slothiesmooth.nyx.shared.presentation.image.toImageBitmap
import com.slothiesmooth.nyx.shared.presentation.state.UiState
import com.slothiesmooth.nyx.shared.presentation.text.UiText
import com.slothiesmooth.nyx.shared.presentation.viewmodel.BaseViewModel

private const val ID_PREFIX_LENGTH = 8
private const val DEFAULT_SHARE_NAME = "nyx-image.png"

/**
 * Drives the encrypt wizard: decode the picked cover into a thumbnail + capacity hint, validate the
 * message/password step, run the encrypt use case, optionally persist to the vault, then expose the
 * saved name for the result step. All decoding, capacity math, naming, and error mapping happen here
 * so the composables only observe render-ready [EncryptState].
 */
class EncryptViewModel(
    private val encryptMessage: EncryptMessageUseCase,
    private val saveToVault: SaveToVaultUseCase,
    private val codec: ImageCodec,
    private val imagePicker: ImagePicker,
    private val cameraSource: CameraSource,
    private val shareSource: ShareSource,
    private val capabilities: PlatformCapabilities,
) : BaseViewModel() {

    private val mutableState = EncryptMutableState(cameraVisible = capabilities.camera && cameraSource.isAvailable)
    val state: EncryptState get() = mutableState

    private var coverBytes: ByteArray? = null
    private var resultBytes: ByteArray? = null
    private var shareFileName: String = DEFAULT_SHARE_NAME

    fun onImagePicked(bytes: ByteArray) {
        async("pick") {
            coverBytes = bytes
            val decoded = codec.decode(bytes)
            val thumb = runCatching { bytes.toImageBitmap() }.getOrNull()
            val label = if (decoded is AppResult.Ok) {
                val fit = estimateMaxMessageChars(decoded.value.width, decoded.value.height)
                UiText.res(Res.string.encrypt_capacity_hint, fit)
            } else {
                null
            }
            withState {
                mutableState.thumbnail = thumb
                mutableState.maxCharsLabel = label
                mutableState.step = EncryptStep.Compose
            }
        }
    }

    fun onPickImage() {
        async("pick-image") { imagePicker.pickImage()?.let { onImagePicked(it.bytes) } }
    }

    fun onCameraCapture() {
        async("camera") { cameraSource.capture()?.let { onImagePicked(it.bytes) } }
    }

    fun onMessageChange(value: String) = updateInput { mutableState.message = value }

    fun onPasswordChange(value: String) = updateInput { mutableState.password = value }

    fun onConfirmChange(value: String) = updateInput { mutableState.confirmPassword = value }

    private inline fun updateInput(crossinline mutate: () -> Unit) = withState {
        mutate()
        val validation = validateEncryptInput(mutableState.message, mutableState.password, mutableState.confirmPassword)
        mutableState.canEncrypt = validation.canEncrypt
        mutableState.validationError = validation.error
    }

    fun encrypt() {
        val cover = coverBytes ?: return
        if (!mutableState.canEncrypt) return
        withState { mutableState.uiState = UiState.Blocking }
        async("encrypt") {
            when (val outcome = encryptMessage(cover, mutableState.message, mutableState.password)) {
                is EncryptOutcome.Success -> onEncryptSuccess(outcome.pngBytes)
                is EncryptOutcome.TooLarge -> withState {
                    mutableState.validationError =
                        UiText.res(Res.string.encrypt_too_large, outcome.requiredChars, outcome.availableChars)
                    mutableState.uiState = UiState.Ready
                }
                EncryptOutcome.Failed -> withState {
                    mutableState.validationError = UiText.res(Res.string.encrypt_failed)
                    mutableState.uiState = UiState.Ready
                }
            }
        }
    }

    private suspend fun onEncryptSuccess(pngBytes: ByteArray) {
        resultBytes = pngBytes
        val saved = if (capabilities.persistentVault) saveToVault(pngBytes, name = "") else null
        val savedId = (saved as? AppResult.Ok)?.value
        val name = savedId?.let { "nyx-${it.value.take(ID_PREFIX_LENGTH)}.png" } ?: DEFAULT_SHARE_NAME
        shareFileName = name
        withState {
            mutableState.savedName = name
            mutableState.step = EncryptStep.Result
            mutableState.uiState = UiState.Ready
        }
    }

    fun share() {
        val bytes = resultBytes ?: return
        async("share") { shareSource.shareImage(bytes, shareFileName) }
    }

    fun back() = withState {
        mutableState.step = when (mutableState.step) {
            EncryptStep.Result -> EncryptStep.Compose
            EncryptStep.Compose -> EncryptStep.PickImage
            EncryptStep.PickImage -> EncryptStep.PickImage
        }
    }

    fun reset() {
        coverBytes = null
        resultBytes = null
        shareFileName = DEFAULT_SHARE_NAME
        withState {
            mutableState.step = EncryptStep.PickImage
            mutableState.thumbnail = null
            mutableState.maxCharsLabel = null
            mutableState.message = ""
            mutableState.password = ""
            mutableState.confirmPassword = ""
            mutableState.validationError = null
            mutableState.canEncrypt = false
            mutableState.savedName = null
        }
    }
}
