package com.slothiesmooth.nyx.feature.encrypt.basic.presentation

import com.slothiesmooth.nyx.feature.encrypt.basic.domain.estimateMaxMessageChars
import com.slothiesmooth.nyx.feature.encrypt.basic.domain.usecase.EncryptMessageUseCase
import com.slothiesmooth.nyx.feature.encrypt.basic.domain.usecase.SaveToVaultUseCase
import com.slothiesmooth.nyx.shared.data.result.AppError
import com.slothiesmooth.nyx.shared.data.result.AppResult
import com.slothiesmooth.nyx.shared.data.source.CameraSource
import com.slothiesmooth.nyx.shared.data.source.ImageCodec
import com.slothiesmooth.nyx.shared.data.source.PlatformCapabilities
import com.slothiesmooth.nyx.shared.data.source.ShareSource
import com.slothiesmooth.nyx.shared.presentation.image.toImageBitmap
import com.slothiesmooth.nyx.shared.presentation.state.UiState
import com.slothiesmooth.nyx.shared.presentation.viewmodel.BaseViewModel

private const val ID_PREFIX_LENGTH = 8
private const val DEFAULT_SHARE_NAME = "nyx-image.png"
private const val ENCRYPT_FAILED = "Could not encrypt this image."

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
                "About ${estimateMaxMessageChars(decoded.value.width, decoded.value.height)} characters fit"
            } else {
                ""
            }
            withState {
                mutableState.thumbnail = thumb
                mutableState.maxCharsLabel = label
                mutableState.step = EncryptStep.Compose
            }
        }
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
            val result = encryptMessage(cover, mutableState.message, mutableState.password)
            if (result is AppResult.Err) {
                val message = (result.cause as? AppError.Validation)?.message ?: ENCRYPT_FAILED
                withState {
                    mutableState.validationError = message
                    mutableState.uiState = UiState.Ready
                }
                return@async
            }
            val pngBytes = (result as AppResult.Ok).value
            resultBytes = pngBytes
            val saved = if (capabilities.persistentVault) saveToVault(pngBytes, name = "") else null
            val savedId = when (saved) {
                is AppResult.Ok -> saved.value
                else -> null
            }
            val name = savedId?.let { "nyx-${it.value.take(ID_PREFIX_LENGTH)}.png" } ?: DEFAULT_SHARE_NAME
            shareFileName = name
            withState {
                mutableState.savedName = name
                mutableState.step = EncryptStep.Result
                mutableState.uiState = UiState.Ready
            }
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
            mutableState.maxCharsLabel = ""
            mutableState.message = ""
            mutableState.password = ""
            mutableState.confirmPassword = ""
            mutableState.validationError = null
            mutableState.canEncrypt = false
            mutableState.savedName = null
        }
    }
}
