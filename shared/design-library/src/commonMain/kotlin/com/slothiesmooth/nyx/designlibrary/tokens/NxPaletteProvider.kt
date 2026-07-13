package com.slothiesmooth.nyx.designlibrary.tokens

import androidx.compose.ui.tooling.preview.PreviewParameterProvider

class NxPaletteProvider : PreviewParameterProvider<NxPalette> {
    override val values: Sequence<NxPalette> = NxPalette.entries.asSequence()
}
