package com.slothiesmooth.nyx.designlibrary.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.slothiesmooth.nyx.designlibrary.atoms.NxIconButton
import com.slothiesmooth.nyx.designlibrary.atoms.NxIconButtonStyle
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.designlibrary.tokens.nxDimensions

@Composable
fun NxTopBarSample() {
    val dimensions = MaterialTheme.nxDimensions
    Column(verticalArrangement = Arrangement.spacedBy(dimensions.keyline4)) {
        NxTopBar(title = "Vault")
        NxTopBar(
            title = "Encrypt",
            subtitle = "Step 2 of 3",
            onBack = {},
            trailing = {
                NxIconButton(
                    kind = NxIconKind.Share,
                    onClick = {},
                    style = NxIconButtonStyle.Ghost,
                    contentDescription = "Share",
                )
            },
        )
    }
}

@AllThemePreview
@Composable
private fun NxTopBarPaletteAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { NxTopBarSample() }
}
