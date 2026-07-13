package com.slothiesmooth.nyx.designlibrary.molecules

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter

private val OverlayPreviewHeight: Dp = 240.dp

@Composable
fun NxProgressOverlaySample() {
    Box(modifier = Modifier.fillMaxWidth().height(OverlayPreviewHeight)) {
        NxProgressOverlay(label = "Encrypting", modifier = Modifier.fillMaxSize())
    }
}

@AllThemePreview
@Composable
private fun NxProgressOverlayPaletteAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { NxProgressOverlaySample() }
}
