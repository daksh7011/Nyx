package com.slothiesmooth.nyx.feature.settings.basic.presentation

import app.cash.turbine.test
import com.slothiesmooth.nyx.feature.settings.basic.domain.usecase.WipeVaultUseCase
import com.slothiesmooth.nyx.shared.data.event.DefaultDomainEventBus
import com.slothiesmooth.nyx.shared.data.source.AppInfo
import com.slothiesmooth.nyx.shared.data.source.StegoImageRecord
import com.slothiesmooth.nyx.shared.testsupport.FakeVaultFileStore
import com.slothiesmooth.nyx.shared.testsupport.FakeVaultSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val WIPE_TIMEOUT_MS = 2_000L

private class TestAppInfo(
    override val versionName: String = "1.0.0",
    override val platformName: String = "Android",
) : AppInfo

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(vaultSource: FakeVaultSource = FakeVaultSource()) = SettingsViewModel(
        wipeVault = WipeVaultUseCase(vaultSource, FakeVaultFileStore(), DefaultDomainEventBus()),
        appInfo = TestAppInfo(),
    )

    @Test
    fun `version label is composed from AppInfo at construction`() {
        assertEquals("Nyx 1.0.0 · Android", viewModel().state.versionLabel)
    }

    @Test
    fun `requestWipe shows the confirm dialog, cancelWipe dismisses it without wiping`() = runTest {
        val viewModel = viewModel()

        viewModel.requestWipe()
        runCurrent()
        assertTrue(viewModel.state.showWipeConfirm)

        viewModel.cancelWipe()
        runCurrent()
        assertFalse(viewModel.state.showWipeConfirm)
        assertFalse(viewModel.state.isWiping)
    }

    /**
     * `confirmWipe` dispatches the actual wipe onto the real `Dispatchers.Default` (fire-and-forget,
     * matching every other feature VM's mutation calls) — over in-memory fakes it can finish before this
     * thread's next line runs, so the mid-flight `isWiping == true` moment is not reliably observable and
     * isn't asserted. This blocks (via [runBlocking]) until [SettingsState.isWiping] actually clears,
     * which both keeps the final assertion deterministic and ensures the background job finishes before
     * [tearDown] resets `Dispatchers.Main` out from under it.
     */
    @Test
    fun `confirmWipe dismisses the dialog, wipes the vault, and clears the wiping flag`() = runBlocking {
        val vaultSource = FakeVaultSource().apply {
            upsert(StegoImageRecord("a", "nyx-a.png", "2026-07-13T00:00:00Z", "2026-07-13T00:00:00Z", null, false))
        }
        val viewModel = viewModel(vaultSource)

        viewModel.confirmWipe()
        assertFalse(viewModel.state.showWipeConfirm)

        withTimeout(WIPE_TIMEOUT_MS) {
            while (viewModel.state.isWiping) delay(1)
        }
        vaultSource.observeActive().test { assertTrue(awaitItem().isEmpty()) }
    }
}
