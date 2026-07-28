package com.slothiesmooth.nyx.client

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.slothiesmooth.nyx.client.data.codec.defaultImageCodec
import com.slothiesmooth.nyx.crypto.DefaultNyxCrypto
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.feature.decrypt.basic.domain.usecase.DecryptMessageUseCase
import com.slothiesmooth.nyx.feature.decrypt.basic.domain.usecase.LoadVaultImageBytesUseCase
import com.slothiesmooth.nyx.feature.decrypt.basic.presentation.DecryptContent
import com.slothiesmooth.nyx.feature.decrypt.basic.presentation.DecryptViewModel
import com.slothiesmooth.nyx.feature.encrypt.basic.domain.EncryptOutcome
import com.slothiesmooth.nyx.feature.encrypt.basic.domain.usecase.EncryptMessageUseCase
import com.slothiesmooth.nyx.shared.data.source.PickedImage
import com.slothiesmooth.nyx.shared.testsupport.FakeClipboardWriter
import com.slothiesmooth.nyx.shared.testsupport.FakeImagePicker
import com.slothiesmooth.nyx.shared.testsupport.FakeVaultFileStore
import com.slothiesmooth.nyx.steganography.Steganography
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

/**
 * Drives the real reveal journey end to end on device: the actual [DecryptContent] UI over a real
 * [DecryptViewModel] backed by real crypto + stego + the real Android [defaultImageCodec]. A stego
 * image (built in setup by the real encrypt path) is supplied through the injectable [FakeImagePicker]
 * exactly as the picker would; the only other doubles are the clipboard and the (unused) vault store.
 * Proves: pick -> password -> reveal shows the plaintext -> copy writes it; and a wrong password shows
 * the honest error instead of the message.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class DecryptFlowDeviceTest {

    private val crypto = DefaultNyxCrypto()
    private val stego = Steganography()
    private val codec = defaultImageCodec()

    private lateinit var imagePicker: FakeImagePicker
    private lateinit var clipboard: FakeClipboardWriter
    private lateinit var viewModel: DecryptViewModel

    @Before
    fun setup() = runBlocking {
        val encrypt = EncryptMessageUseCase(crypto, stego, codec)
        val stegoBytes = (encrypt(generateCoverPng(), MESSAGE, PASSWORD) as EncryptOutcome.Success).pngBytes
        imagePicker = FakeImagePicker(next = PickedImage(bytes = stegoBytes, suggestedName = "stego.png"))
        clipboard = FakeClipboardWriter()
        viewModel = DecryptViewModel(
            decryptMessage = DecryptMessageUseCase(crypto, stego, codec),
            loadVaultImageBytes = LoadVaultImageBytesUseCase(FakeVaultFileStore()),
            imagePicker = imagePicker,
            clipboardWriter = clipboard,
        )
    }

    @Test
    fun picksRevealsAndCopiesTheHiddenMessage() = runComposeUiTest {
        renderDecryptContent()

        onNodeWithText(CHOOSE_IMAGE).performClick()
        waitUntil(timeoutMillis = TIMEOUT_MILLIS) { viewModel.state.hasImage }

        onNode(hasSetTextAction()).performTextInput(PASSWORD)
        waitUntil(timeoutMillis = TIMEOUT_MILLIS) { viewModel.state.canDecrypt }

        onNodeWithText(REVEAL).performClick()
        waitUntil(timeoutMillis = TIMEOUT_MILLIS) { viewModel.state.plaintext == MESSAGE }
        onNodeWithText(MESSAGE).assertIsDisplayed()

        onNodeWithContentDescription(COPY_DESCRIPTION).performClick()
        waitUntil(timeoutMillis = TIMEOUT_MILLIS) { clipboard.lastCopied == MESSAGE }
        assertEquals(MESSAGE, clipboard.lastCopied)
    }

    @Test
    fun wrongPasswordShowsTheHonestErrorInsteadOfTheMessage() = runComposeUiTest {
        renderDecryptContent()

        onNodeWithText(CHOOSE_IMAGE).performClick()
        waitUntil(timeoutMillis = TIMEOUT_MILLIS) { viewModel.state.hasImage }

        onNode(hasSetTextAction()).performTextInput(WRONG_PASSWORD)
        waitUntil(timeoutMillis = TIMEOUT_MILLIS) { viewModel.state.canDecrypt }

        onNodeWithText(REVEAL).performClick()
        waitUntil(timeoutMillis = TIMEOUT_MILLIS) { viewModel.state.errorMessage != null }
        onNodeWithText(WRONG_PASSWORD_ERROR).assertIsDisplayed()
    }

    private fun ComposeUiTest.renderDecryptContent() = setContent {
        NxTheme(NxPalette.Nardo) {
            DecryptContent(
                state = viewModel.state,
                onPickImage = viewModel::onPickImage,
                onPasswordChange = viewModel::onPasswordChange,
                onDecrypt = viewModel::decrypt,
                onCopy = viewModel::onCopy,
            )
        }
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
        const val MESSAGE = "meet at the old bridge at midnight"
        const val PASSWORD = "correct horse battery staple"
        const val WRONG_PASSWORD = "not the password"
        const val WRONG_PASSWORD_ERROR = "Wrong password, or this image has been tampered with."
        const val CHOOSE_IMAGE = "Choose image"
        const val REVEAL = "Reveal message"
        const val COPY_DESCRIPTION = "Copy message"
        const val COVER_SIZE = 128
        const val PNG_QUALITY = 100
        const val TIMEOUT_MILLIS = 5_000L
    }
}
