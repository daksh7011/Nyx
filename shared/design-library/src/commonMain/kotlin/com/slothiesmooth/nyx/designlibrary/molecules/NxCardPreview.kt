package com.slothiesmooth.nyx.designlibrary.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.slothiesmooth.nyx.designlibrary.atoms.NxText
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTextStyle
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.designlibrary.tokens.nxColors
import com.slothiesmooth.nyx.designlibrary.tokens.nxDimensions
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter

private val SampleWidth = 280.dp

@Composable
fun NxCardSample() {
    val colors = MaterialTheme.nxColors
    val dimensions = MaterialTheme.nxDimensions
    Column(
        modifier = Modifier.padding(dimensions.keyline4).width(SampleWidth),
        verticalArrangement = Arrangement.spacedBy(dimensions.keyline3),
    ) {
        NxCard(variant = NxCardVariant.Elevated) {
            NxText("Elevated card", style = NxTextStyle.BodyStrong)
            NxText("Border, shadow, elevated surface", style = NxTextStyle.Caption, color = colors.fgSubtle)
        }
        NxCard(variant = NxCardVariant.Flat) {
            NxText("Flat card", style = NxTextStyle.BodyStrong)
            NxText("Divider border, no shadow", style = NxTextStyle.Caption, color = colors.fgSubtle)
        }
        NxCard(variant = NxCardVariant.Elevated, onClick = {}) {
            NxText("Clickable card", style = NxTextStyle.BodyStrong)
            NxText("Whole surface is a tap target", style = NxTextStyle.Caption, color = colors.fgSubtle)
        }
    }
}

@AllThemePreview
@Composable
private fun NxCardPaletteAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { NxCardSample() }
}
