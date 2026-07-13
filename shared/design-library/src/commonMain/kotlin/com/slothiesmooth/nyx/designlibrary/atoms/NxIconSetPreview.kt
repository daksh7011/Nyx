package com.slothiesmooth.nyx.designlibrary.atoms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTextStyle
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.designlibrary.tokens.nxColors
import com.slothiesmooth.nyx.designlibrary.tokens.nxDimensions

private const val ICONS_PER_ROW = 6

@Composable
fun NxIconSetSample() {
    val colors = MaterialTheme.nxColors
    val rows = NxIconKind.entries.chunked(ICONS_PER_ROW)
    Column(
        modifier = Modifier.padding(MaterialTheme.nxDimensions.keyline4),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.nxDimensions.keyline3),
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.nxDimensions.keyline3)) {
                row.forEach { kind ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        NxIcon(kind = kind, tint = colors.fg)
                        NxText(text = kind.name, style = NxTextStyle.Caption, color = colors.fgMuted)
                    }
                }
            }
        }
    }
}

@AllThemePreview
@Composable
private fun NxIconSetPaletteAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { NxIconSetSample() }
}
