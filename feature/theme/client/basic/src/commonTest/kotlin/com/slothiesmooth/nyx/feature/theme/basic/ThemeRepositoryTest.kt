package com.slothiesmooth.nyx.feature.theme.basic

import app.cash.turbine.test
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.feature.theme.api.ThemeMode
import com.slothiesmooth.nyx.shared.testsupport.FakeSettingsSource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeRepositoryTest {

    @Test
    fun `defaults to System mode with the default dark and light palettes`() = runTest {
        val repository = ThemeRepository(FakeSettingsSource())
        repository.observeConfig().test {
            val config = awaitItem()
            assertEquals(ThemeMode.System, config.mode)
            assertEquals(NxPalette.DefaultDark, config.darkPalette)
            assertEquals(NxPalette.DefaultLight, config.lightPalette)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setMode persists and is observed`() = runTest {
        val repository = ThemeRepository(FakeSettingsSource())
        repository.setMode(ThemeMode.Dark)
        repository.observeConfig().test {
            assertEquals(ThemeMode.Dark, awaitItem().mode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setPalette routes a palette to its own slot and leaves the other slot default`() = runTest {
        val repository = ThemeRepository(FakeSettingsSource())
        repository.setPalette(NxPalette.Espresso)
        repository.observeConfig().test {
            val config = awaitItem()
            assertEquals(NxPalette.Espresso, config.darkPalette)
            assertEquals(NxPalette.DefaultLight, config.lightPalette)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a palette id in the wrong slot or an unknown id falls back to that slot default`() = runTest {
        val settings = FakeSettingsSource(
            mapOf(
                "theme.dark" to NxPalette.Cream.name,
                "theme.light" to "NoSuchPalette",
            ),
        )
        val repository = ThemeRepository(settings)
        repository.observeConfig().test {
            val config = awaitItem()
            assertEquals(NxPalette.DefaultDark, config.darkPalette)
            assertEquals(NxPalette.DefaultLight, config.lightPalette)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
