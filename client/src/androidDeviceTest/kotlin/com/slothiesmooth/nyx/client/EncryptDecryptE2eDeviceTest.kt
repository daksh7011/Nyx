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
import androidx.test.platform.app.InstrumentationRegistry
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.slothiesmooth.nyx.client.data.codec.defaultImageCodec
import com.slothiesmooth.nyx.client.data.source.database.sqldelight.SqlDelightSource
import com.slothiesmooth.nyx.client.data.source.database.vault.VaultSqlSource
import com.slothiesmooth.nyx.client.data.sqldelight.NyxDb
import com.slothiesmooth.nyx.crypto.DefaultNyxCrypto
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.feature.decrypt.basic.domain.DecryptOutcome
import com.slothiesmooth.nyx.feature.decrypt.basic.domain.usecase.DecryptMessageUseCase
import com.slothiesmooth.nyx.feature.encrypt.basic.domain.usecase.EncryptMessageUseCase
import com.slothiesmooth.nyx.feature.encrypt.basic.domain.usecase.SaveToVaultUseCase
import com.slothiesmooth.nyx.feature.encrypt.basic.presentation.EncryptContent
import com.slothiesmooth.nyx.feature.encrypt.basic.presentation.EncryptStep
import com.slothiesmooth.nyx.feature.encrypt.basic.presentation.EncryptViewModel
import com.slothiesmooth.nyx.shared.data.event.DefaultDomainEventBus
import com.slothiesmooth.nyx.shared.data.result.AppResult
import com.slothiesmooth.nyx.shared.data.source.PickedImage
import com.slothiesmooth.nyx.shared.data.source.PlatformCapabilities
import com.slothiesmooth.nyx.shared.data.source.VaultSource
import com.slothiesmooth.nyx.shared.testsupport.FakeCameraSource
import com.slothiesmooth.nyx.shared.testsupport.FakeImagePicker
import com.slothiesmooth.nyx.shared.testsupport.FakeShareSource
import com.slothiesmooth.nyx.shared.testsupport.id.DeterministicIdGenerator
import com.slothiesmooth.nyx.shared.testsupport.time.FakeClock
import com.slothiesmooth.nyx.steganography.Steganography
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.manualFileKitCoreInitialization
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import kotlin.time.Instant

/**
 * The real end-to-end journey on device, with NO fake data layer: the user drives the actual encrypt
 * wizard, the stego PNG is written to the app's internal scoped storage
 * (`/data/data/<pkg>/files/...`) through the production [FileKitVaultFileStore], and a metadata row
 * lands in a real SqlDelight database. The test then reads the row back through the real
 * [VaultSqlSource], loads the bytes back off disk, and decrypts them through the real engine to
 * recover the original message. Only the image picker (an un-automatable OS surface) is a byte-source
 * seam. This is the guard that would have caught the empty-vault bug the old fake-store test missed.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class EncryptDecryptE2eDeviceTest {

    private val crypto = DefaultNyxCrypto()
    private val stego = Steganography()
    private val codec = defaultImageCodec()
    private val decrypt = DecryptMessageUseCase(crypto, stego, codec)

    private lateinit var imagePicker: FakeImagePicker
    private lateinit var vaultFileStore: FileKitVaultFileStore
    private lateinit var vaultSource: VaultSource
    private lateinit var viewModel: EncryptViewModel

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        FileKit.manualFileKitCoreInitialization(context)

        // Fresh install: the vault directory must not exist and the DB must be empty.
        val vaultRoot = PlatformFile(FileKit.filesDir, VAULT_DIR)
        vaultFileStore = FileKitVaultFileStore(vaultRoot)
        runBlocking {
            vaultFileStore.deleteAll()
            if (vaultRoot.exists()) vaultRoot.delete()
        }
        context.deleteDatabase(DATABASE_NAME)

        imagePicker = FakeImagePicker(next = PickedImage(bytes = generateCoverPng(), suggestedName = "cover.png"))
        val driver = AndroidSqliteDriver(NyxDb.Schema.synchronous(), context, DATABASE_NAME)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        vaultSource = VaultSqlSource(SqlDelightSource(driver, scope))

        viewModel = EncryptViewModel(
            encryptMessage = EncryptMessageUseCase(crypto, stego, codec),
            saveToVault = SaveToVaultUseCase(
                vaultSource = vaultSource,
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
    fun encryptsThroughTheWizardSavesToRealVaultThenDecryptsBackFromIt() = runComposeUiTest {
        renderEncryptContent()

        onNodeWithText(CHOOSE_IMAGE).performClick()
        waitUntil(timeoutMillis = TIMEOUT_MILLIS) { viewModel.state.hasImage }
        onNode(hasText(CONTINUE) and hasClickAction()).performClick()
        waitUntil(timeoutMillis = TIMEOUT_MILLIS) { viewModel.state.step == EncryptStep.Message }

        onAllNodes(hasSetTextAction())[MESSAGE_FIELD].performTextInput(MESSAGE)
        waitUntil(timeoutMillis = TIMEOUT_MILLIS) { viewModel.state.messageReady }
        onNode(hasText(CONTINUE) and hasClickAction()).performClick()
        waitUntil(timeoutMillis = TIMEOUT_MILLIS) { viewModel.state.step == EncryptStep.Password }

        typePasswords(password = PASSWORD, confirm = PASSWORD)
        waitUntil(timeoutMillis = TIMEOUT_MILLIS) { viewModel.state.canEncrypt }
        onNode(hasText(ENCRYPT_ACTION) and hasClickAction()).performClick()
        waitUntil(timeoutMillis = TIMEOUT_MILLIS) { viewModel.state.step == EncryptStep.Result }
        onNodeWithText(RESULT_HEADING).assertIsDisplayed()

        // Real read path: exactly one active row is in the real database.
        val active = runBlocking { vaultSource.observeActive().first() }
        assertEquals("exactly one active row must be persisted to the real database", 1, active.size)
        val savedId = active.first().id

        // Real bytes are on real internal scoped storage, and they decrypt back to the original message.
        val stored = runBlocking { vaultFileStore.read(savedId) }
        assertTrue("stego bytes must exist in internal scoped storage", stored is AppResult.Ok)
        val savedBytes = (stored as AppResult.Ok).value
        assertEquals(DecryptOutcome.Success(MESSAGE), runBlocking { decrypt(savedBytes, PASSWORD) })
    }

    private fun ComposeUiTest.renderEncryptContent() = setContent {
        NxTheme(NxPalette.Nardo) {
            EncryptContent(
                state = viewModel.state,
                onPickImage = viewModel::onPickImage,
                onCaptureImage = viewModel::onCameraCapture,
                onImageContinue = viewModel::onImageContinue,
                onMessageChange = viewModel::onMessageChange,
                onMessageContinue = viewModel::onMessageContinue,
                onPasswordChange = viewModel::onPasswordChange,
                onConfirmChange = viewModel::onConfirmChange,
                onEncrypt = viewModel::encrypt,
                onShare = viewModel::share,
                onReset = viewModel::reset,
            )
        }
    }

    private fun ComposeUiTest.typePasswords(password: String, confirm: String) {
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
        const val VAULT_DIR = "stego_vault_e2e"
        const val DATABASE_NAME = "encrypt-decrypt-e2e.db"
        const val CHOOSE_IMAGE = "Choose image"
        const val CONTINUE = "Continue"
        const val ENCRYPT_ACTION = "Encrypt"
        const val RESULT_HEADING = "Encrypted and saved"
        const val MESSAGE_FIELD = 0
        const val PASSWORD_FIELD = 0
        const val CONFIRM_FIELD = 1
        const val COVER_SIZE = 128
        const val PNG_QUALITY = 100
        const val TIMEOUT_MILLIS = 5_000L
    }
}
