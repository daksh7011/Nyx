package com.slothiesmooth.nyx.feature.theme.basic.previews

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.feature.theme.api.ThemeMode
import com.slothiesmooth.nyx.feature.theme.basic.ChangeThemeMutableState
import com.slothiesmooth.nyx.feature.theme.basic.ChangeThemeScreenStateless
import com.slothiesmooth.nyx.feature.theme.basic.ChangeThemeState

private fun sampleState(mode: ThemeMode): ChangeThemeState =
    ChangeThemeMutableState().apply { this.mode = mode }

@AllThemePreview
@Composable
private fun ChangeThemeSystemAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { ChangeThemeScreenStateless(sampleState(ThemeMode.System)) }
}

@AllThemePreview
@Composable
private fun ChangeThemeDarkAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { ChangeThemeScreenStateless(sampleState(ThemeMode.Dark)) }
}
