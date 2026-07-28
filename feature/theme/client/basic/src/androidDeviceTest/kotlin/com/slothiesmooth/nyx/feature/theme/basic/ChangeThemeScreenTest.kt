package com.slothiesmooth.nyx.feature.theme.basic

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.slothiesmooth.nyx.designlibrary.atoms.NxText
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.shared.testsupport.FakeSettingsSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.Test
import org.junit.runner.RunWith

private const val DARK_PALETTE_STATUS_TAG = "dark-palette-status"
private const val THEME_CHANGE_TIMEOUT_MILLIS = 5_000L

/**
 * Drives the real change-theme stack end to end: [ChangeThemeScreen] over a real [ThemeViewModel]
 * bound to a real [BasicThemeProvider] (the [com.slothiesmooth.nyx.feature.theme.api.ThemeFeature])
 * over a real [ThemeRepository]. The only double is [FakeSettingsSource], the external persistence
 * seam. Tapping a palette card must persist and round-trip back through the feature's theme flow.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class ChangeThemeScreenTest {

    @Test
    fun selectingAPaletteCardPersistsAndReflectsThroughTheFeature() = runComposeUiTest {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val provider = BasicThemeProvider(ThemeRepository(FakeSettingsSource()), scope)
        val viewModel = ThemeViewModel(ChangeThemeMutableState(), provider)

        setContent {
            viewModel.bind()
            val config by provider.theme.collectAsState()
            ThemeProvider(themeFeature = provider) {
                Column {
                    NxText(
                        text = config.darkPalette.name,
                        modifier = Modifier.testTag(DARK_PALETTE_STATUS_TAG),
                    )
                    ChangeThemeScreen(viewModel)
                }
            }
        }

        onNodeWithText(NxPalette.Espresso.displayName).performClick()

        waitUntil(timeoutMillis = THEME_CHANGE_TIMEOUT_MILLIS) {
            runCatching {
                onNodeWithTag(DARK_PALETTE_STATUS_TAG).assertTextEquals(NxPalette.Espresso.name)
            }.isSuccess
        }
        onNodeWithTag(DARK_PALETTE_STATUS_TAG).assertTextEquals(NxPalette.Espresso.name)
    }
}
