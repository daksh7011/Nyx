package com.slothiesmooth.nyx.client

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.slothiesmooth.nyx.client.data.codec.defaultImageCodec
import com.slothiesmooth.nyx.crypto.DefaultNyxCrypto
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.feature.encrypt.basic.domain.usecase.EncryptMessageUseCase
import com.slothiesmooth.nyx.feature.encrypt.basic.domain.usecase.SaveToVaultUseCase
import com.slothiesmooth.nyx.feature.encrypt.basic.presentation.EncryptContent
import com.slothiesmooth.nyx.feature.encrypt.basic.presentation.EncryptStep
import com.slothiesmooth.nyx.feature.encrypt.basic.presentation.EncryptViewModel
import com.slothiesmooth.nyx.shared.data.event.DefaultDomainEventBus
import com.slothiesmooth.nyx.shared.data.result.AppResult
import com.slothiesmooth.nyx.shared.data.source.PickedImage
import com.slothiesmooth.nyx.shared.data.source.PlatformCapabilities
import com.slothiesmooth.nyx.shared.testsupport.FakeCameraSource
import com.slothiesmooth.nyx.shared.testsupport.FakeImagePicker
import com.slothiesmooth.nyx.shared.testsupport.FakeShareSource
import com.slothiesmooth.nyx.shared.testsupport.FakeVaultFileStore
import com.slothiesmooth.nyx.shared.testsupport.FakeVaultSource
import com.slothiesmooth.nyx.shared.testsupport.id.DeterministicIdGenerator
import com.slothiesmooth.nyx.shared.testsupport.time.FakeClock
import com.slothiesmooth.nyx.steganography.Steganography
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import kotlin.time.Instant

/**
 * Drives the real encrypt wizard end to end on device: the actual [EncryptContent] over a real
 * [EncryptViewModel] (real crypto + stego + the real Android [defaultImageCodec], a real
 * SaveToVaultUseCase over fake vault stores). A cover is supplied through [FakeImagePicker] as the
 * picker would. Proves: pick -> compose (message + matching passwords) -> Encrypt advances to the
 * saved result AND the stego bytes land in the vault store; and a password mismatch blocks encrypt
 * with the honest inline error.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class EncryptFlowDeviceTest {

    private val crypto = DefaultNyxCrypto()
    private val stego = Steganography()
    private val codec = defaultImageCodec()

    private lateinit var imagePicker: FakeImagePicker
    private lateinit var vaultFileStore: FakeVaultFileStore
    private lateinit var viewModel: EncryptViewModel

    @Before
    fun setup() {
        imagePicker = FakeImagePicker(next = PickedImage(bytes = generateCoverPng(), suggestedName = "cover.png"))
        vaultFileStore = FakeVaultFileStore()
        viewModel = EncryptViewModel(
            encryptMessage = EncryptMessageUseCase(crypto, stego, codec),
            saveToVault = SaveToVaultUseCase(
                vaultSource = FakeVaultSource(),
                fileStore = vaultFileStore,
                idGenerator = DeterministicIdGenerator(),
                clock = FakeClock(Instant.parse(FIXED_INSTANT)),
                eventBus = DefaultDomainEventBus(),
            ),
            codec = codec,
            imagePicker = imagePicker,
            cameraSource = FakeCameraSource(),
            shareSource = FakeShareSource(),
            capabilities = PlatformCapabilities(camera = false, persistentVault = true),
        )
    }

    @Test
    fun picksComposesEncryptsAndPersistsToTheVault() = runComposeUiTest {
        renderEncryptContent()

        onNodeWithText(CHOOSE_IMAGE).performClick()
        waitUntil(timeoutMillis = TIMEOUT_MILLIS) { viewModel.state.step == EncryptStep.Compose }

        typeComposeStep(password = PASSWORD, confirm = PASSWORD)
        waitUntil(timeoutMillis = TIMEOUT_MILLIS) { viewModel.state.canEncrypt }

        onNode(hasText(ENCRYPT_ACTION) and hasClickAction()).performClick()
        waitUntil(timeoutMillis = TIMEOUT_MILLIS) { viewModel.state.step == EncryptStep.Result }

        onNodeWithText(RESULT_HEADING).assertIsDisplayed()
        val stored = runBlocking { vaultFileStore.read(FIRST_ID) }
        assertTrue("the stego bytes must land in the vault store", stored is AppResult.Ok)
    }

    @Test
    fun mismatchedPasswordsBlockEncryptWithTheInlineError() = runComposeUiTest {
        renderEncryptContent()

        onNodeWithText(CHOOSE_IMAGE).performClick()
        waitUntil(timeoutMillis = TIMEOUT_MILLIS) { viewModel.state.step == EncryptStep.Compose }

        typeComposeStep(password = PASSWORD, confirm = "different")
        waitUntil(timeoutMillis = TIMEOUT_MILLIS) { viewModel.state.validationError != null }

        onNodeWithText(PASSWORDS_DO_NOT_MATCH).assertIsDisplayed()
        assertFalse("encrypt must stay blocked on a mismatch", viewModel.state.canEncrypt)
    }

    private fun ComposeUiTest.renderEncryptContent() = setContent {
        NxTheme(NxPalette.Nardo) {
            EncryptContent(
                state = viewModel.state,
                onPickImage = viewModel::onPickImage,
                onCaptureImage = viewModel::onCameraCapture,
                onMessageChange = viewModel::onMessageChange,
                onPasswordChange = viewModel::onPasswordChange,
                onConfirmChange = viewModel::onConfirmChange,
                onEncrypt = viewModel::encrypt,
                onShare = viewModel::share,
                onReset = viewModel::reset,
            )
        }
    }

    /** Fills the compose step's three fields in order: message, password, confirm. */
    private fun ComposeUiTest.typeComposeStep(password: String, confirm: String) {
        onAllNodes(hasSetTextAction())[MESSAGE_FIELD].performTextInput(MESSAGE)
        onAllNodes(hasSetTextAction())[PASSWORD_FIELD].performTextInput(password)
        onAllNodes(hasSetTextAction())[CONFIRM_FIELD].performTextInput(confirm)
    }

    private fun generateCoverPng(): ByteArray {
        val bitmap = Bitmap.createBitmap(COVER_SIZE, COVER_SIZE, Bitmap.Config.ARGB_8888)
        for (y in 0 until COVER_SIZE) {
            for (x in 0 until COVER_SIZE) {
                bitmap.setPixel(x, y, Color.rgb(x, y, x + y))
            }
        }
        return ByteArrayOutputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, stream)
            stream.toByteArray()
        }
    }

    private companion object {
        const val MESSAGE = "the eagle lands at dawn"
        const val PASSWORD = "correct horse battery staple"
        const val FIXED_INSTANT = "2026-07-13T12:00:00Z"
        const val FIRST_ID = "id-0"
        const val CHOOSE_IMAGE = "Choose image"
        const val ENCRYPT_ACTION = "Encrypt"
        const val RESULT_HEADING = "Encrypted and saved"
        const val PASSWORDS_DO_NOT_MATCH = "Passwords do not match"
        const val MESSAGE_FIELD = 0
        const val PASSWORD_FIELD = 1
        const val CONFIRM_FIELD = 2
        const val COVER_SIZE = 128
        const val PNG_QUALITY = 100
        const val TIMEOUT_MILLIS = 5_000L
    }
}
