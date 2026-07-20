package com.slothiesmooth.nyx.client

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.feature.encrypt.basic.domain.usecase.SaveToVaultUseCase
import com.slothiesmooth.nyx.feature.settings.basic.domain.usecase.WipeVaultUseCase
import com.slothiesmooth.nyx.feature.settings.basic.presentation.SettingsContent
import com.slothiesmooth.nyx.feature.settings.basic.presentation.SettingsViewModel
import com.slothiesmooth.nyx.shared.data.event.DefaultDomainEventBus
import com.slothiesmooth.nyx.shared.data.result.AppResult
import com.slothiesmooth.nyx.shared.data.source.AppInfo
import com.slothiesmooth.nyx.shared.testsupport.FakeVaultFileStore
import com.slothiesmooth.nyx.shared.testsupport.FakeVaultSource
import com.slothiesmooth.nyx.shared.testsupport.id.DeterministicIdGenerator
import com.slothiesmooth.nyx.shared.testsupport.time.FakeClock
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import kotlin.time.Instant

/**
 * Drives the real settings wipe journey end to end on device: the actual [SettingsContent] over a
 * real [SettingsViewModel] backed by the real [WipeVaultUseCase] (fake source + file store), with one
 * image seeded through the real [SaveToVaultUseCase]. Proves: Wipe vault -> confirm hard-deletes every
 * row AND the file bytes; and that canceling the confirm dialog leaves the vault untouched.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class SettingsFlowDeviceTest {

    private val vaultSource = FakeVaultSource()
    private val fileStore = FakeVaultFileStore()
    private val eventBus = DefaultDomainEventBus()
    private val clock = FakeClock(Instant.parse(FIXED_INSTANT))

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() = runBlocking {
        val save = SaveToVaultUseCase(vaultSource, fileStore, DeterministicIdGenerator(), clock, eventBus)
        save(generateCoverPng(), IMAGE_NAME)
        viewModel = SettingsViewModel(
            wipeVault = WipeVaultUseCase(vaultSource, fileStore, eventBus),
            appInfo = TestAppInfo,
        )
    }

    @Test
    fun confirmingWipeHardDeletesTheVault() = runComposeUiTest {
        assertEquals("precondition: one image is stored", 1, runBlocking { vaultSource.countActive() })
        renderSettings()

        onNodeWithText(WIPE_VAULT).performClick()
        waitUntil(timeoutMillis = TIMEOUT_MILLIS) { viewModel.state.showWipeConfirm }
        onNodeWithText(CONFIRM_TITLE).assertIsDisplayed()

        onNodeWithText(CONFIRM_ACTION).performClick()
        waitUntil(timeoutMillis = TIMEOUT_MILLIS) { runBlocking { vaultSource.countActive() } == 0 }

        assertEquals("wipe purges every row", 0, runBlocking { vaultSource.countActive() })
        assertTrue("wipe deletes the file bytes", runBlocking { fileStore.read(SEEDED_ID) } is AppResult.Err)
    }

    @Test
    fun cancelingWipeKeepsTheVault() = runComposeUiTest {
        renderSettings()

        onNodeWithText(WIPE_VAULT).performClick()
        waitUntil(timeoutMillis = TIMEOUT_MILLIS) { viewModel.state.showWipeConfirm }

        onNodeWithText(CANCEL).performClick()
        waitUntil(timeoutMillis = TIMEOUT_MILLIS) { !viewModel.state.showWipeConfirm }

        assertEquals("canceling leaves every image intact", 1, runBlocking { vaultSource.countActive() })
    }

    private fun ComposeUiTest.renderSettings() = setContent {
        NxTheme(NxPalette.Nardo) {
            SettingsContent(
                state = viewModel.state,
                onRequestWipe = viewModel::requestWipe,
                onConfirmWipe = viewModel::confirmWipe,
                onCancelWipe = viewModel::cancelWipe,
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

    private object TestAppInfo : AppInfo {
        override val versionName: String = "1.0.0"
        override val platformName: String = "Android"
    }

    private companion object {
        const val FIXED_INSTANT = "2026-07-13T12:00:00Z"
        const val IMAGE_NAME = "nyx-0001.png"
        const val SEEDED_ID = "id-0"
        const val WIPE_VAULT = "Wipe vault"
        const val CONFIRM_TITLE = "Wipe vault?"
        const val CONFIRM_ACTION = "Wipe"
        const val CANCEL = "Cancel"
        const val COVER_SIZE = 128
        const val PNG_QUALITY = 100
        const val TIMEOUT_MILLIS = 5_000L
    }
}
