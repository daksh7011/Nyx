package com.slothiesmooth.nyx.designlibrary.atoms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.designlibrary.tokens.nxDimensions

@Composable
fun NxIconButtonSample() {
    Row(
        modifier = Modifier.padding(MaterialTheme.nxDimensions.keyline4),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.nxDimensions.keyline3),
    ) {
        NxIconButton(kind = NxIconKind.Plus, onClick = {}, style = NxIconButtonStyle.Outline)
        NxIconButton(kind = NxIconKind.Share, onClick = {}, style = NxIconButtonStyle.Ghost)
        NxIconButton(kind = NxIconKind.Camera, onClick = {}, style = NxIconButtonStyle.Filled)
        NxIconButton(kind = NxIconKind.Trash, onClick = {}, style = NxIconButtonStyle.Outline)
    }
}

@AllThemePreview
@Composable
private fun NxIconButtonPaletteAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { NxIconButtonSample() }
}
