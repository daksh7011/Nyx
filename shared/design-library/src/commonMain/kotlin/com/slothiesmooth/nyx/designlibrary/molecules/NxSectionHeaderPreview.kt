package com.slothiesmooth.nyx.designlibrary.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.designlibrary.tokens.nxDimensions
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter

@Composable
fun NxSectionHeaderSample() {
    val dimensions = MaterialTheme.nxDimensions
    Column(
        modifier = Modifier.padding(dimensions.keyline4),
        verticalArrangement = Arrangement.spacedBy(dimensions.keyline3),
    ) {
        NxSectionHeader(title = "Archived")
        NxSectionHeader(title = "Recent", actionText = "See all", onAction = {})
    }
}

@AllThemePreview
@Composable
private fun NxSectionHeaderPaletteAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { NxSectionHeaderSample() }
}
