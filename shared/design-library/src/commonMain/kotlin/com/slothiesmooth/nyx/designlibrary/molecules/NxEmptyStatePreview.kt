package com.slothiesmooth.nyx.designlibrary.molecules

import androidx.compose.runtime.Composable
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter

@Composable
fun NxEmptyStateSample() {
    NxEmptyState(
        icon = NxIconKind.Vault,
        title = "Your vault is empty",
        body = "Hidden messages you save will appear here.",
        ctaText = "Encrypt a message",
        onCta = {},
    )
}

@AllThemePreview
@Composable
private fun NxEmptyStatePaletteAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { NxEmptyStateSample() }
}
