package com.slothiesmooth.nyx.feature.theme.basic

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.feature.theme.api.ThemeConfig
import com.slothiesmooth.nyx.feature.theme.api.ThemeFeature
import com.slothiesmooth.nyx.feature.theme.api.ThemeMode

/** Wraps [content] in `NxTheme` using the palette chosen by the live [ThemeFeature.theme]. */
@Composable
fun ThemeProvider(themeFeature: ThemeFeature, content: @Composable () -> Unit) {
    val config by themeFeature.theme.collectAsState()
    val systemDark = isSystemInDarkTheme()
    NxTheme(palette = effectivePalette(config, systemDark), content = content)
}

private fun effectivePalette(config: ThemeConfig, systemDark: Boolean): NxPalette = when (config.mode) {
    ThemeMode.System -> if (systemDark) config.darkPalette else config.lightPalette
    ThemeMode.Dark -> config.darkPalette
    ThemeMode.Light -> config.lightPalette
}
