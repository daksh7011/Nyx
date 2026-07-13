package com.slothiesmooth.nyx.designlibrary.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.designlibrary.tokens.nxDimensions
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter

@Composable
fun NxFabSample() {
    val dimensions = MaterialTheme.nxDimensions
    Row(
        modifier = Modifier.padding(dimensions.keyline4),
        horizontalArrangement = Arrangement.spacedBy(dimensions.keyline3),
    ) {
        NxFab(icon = NxIconKind.Plus, onClick = {}, contentDescription = "Encrypt a message")
        NxFab(icon = NxIconKind.Camera, onClick = {}, contentDescription = "Capture")
    }
}

@AllThemePreview
@Composable
private fun NxFabPaletteAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { NxFabSample() }
}
