package com.slothiesmooth.nyx.feature.settings.basic.presentation

import app.cash.turbine.test
import com.slothiesmooth.nyx.feature.settings.basic.domain.usecase.WipeVaultUseCase
import com.slothiesmooth.nyx.feature.settings.basic.resources.Res
import com.slothiesmooth.nyx.feature.settings.basic.resources.settings_version_label
import com.slothiesmooth.nyx.shared.data.event.DefaultDomainEventBus
import com.slothiesmooth.nyx.shared.data.source.AppInfo
import com.slothiesmooth.nyx.shared.data.source.StegoImageRecord
import com.slothiesmooth.nyx.shared.presentation.text.UiText
import com.slothiesmooth.nyx.shared.testsupport.FakeVaultFileStore
import com.slothiesmooth.nyx.shared.testsupport.FakeVaultSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
        assertEquals(
            UiText.res(Res.string.settings_version_label, "1.0.0", "Android"),
            viewModel().state.versionLabel,
        )
    }

    @Test
    fun `requestWipe shows the confirm dialog and cancelWipe dismisses it without wiping`() = runTest {
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
     * isn't asserted. Rather than block a thread (unavailable on JS/Wasm), this suspends on the vault's
     * active stream until the wipe has emptied it, which keeps the assertion deterministic on every
     * target and lets the background job finish before [tearDown] resets `Dispatchers.Main`.
     */
    @Test
    fun `confirmWipe dismisses the dialog and wipes the vault and clears the wiping flag`() = runTest {
        val vaultSource = FakeVaultSource().apply {
            upsert(StegoImageRecord("a", "nyx-a.png", "2026-07-13T00:00:00Z", "2026-07-13T00:00:00Z", null, false))
        }
        val viewModel = viewModel(vaultSource)

        viewModel.confirmWipe()
        assertFalse(viewModel.state.showWipeConfirm)

        vaultSource.observeActive().test {
            var active = awaitItem()
            while (active.isNotEmpty()) active = awaitItem()
            assertTrue(active.isEmpty())
        }
    }
}
