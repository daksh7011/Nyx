package com.slothiesmooth.nyx.designlibrary.atoms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTextStyle
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.designlibrary.tokens.nxColors
import com.slothiesmooth.nyx.designlibrary.tokens.nxDimensions

@Composable
fun NxTextSample() {
    Column(
        modifier = Modifier.padding(MaterialTheme.nxDimensions.keyline4),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.nxDimensions.keyline3),
    ) {
        NxText("Hidden in plain sight.", style = NxTextStyle.Display)
        NxText("Vault", style = NxTextStyle.Title)
        NxText("Encrypt a message", style = NxTextStyle.Heading)
        NxText("Top bar title", style = NxTextStyle.Subhead)
        NxText("Standard body text inside cards.", style = NxTextStyle.Body)
        NxText("Body strong — same size, w600.", style = NxTextStyle.BodyStrong)
        NxText("Subtle line under titles.", style = NxTextStyle.Caption, color = MaterialTheme.nxColors.fgMuted)
        NxText("section label", style = NxTextStyle.Kicker, color = MaterialTheme.nxColors.fgSubtle)
        NxText("12 images", style = NxTextStyle.Mono)
    }
}

@AllThemePreview
@Composable
private fun NxTextPaletteAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { NxTextSample() }
}
