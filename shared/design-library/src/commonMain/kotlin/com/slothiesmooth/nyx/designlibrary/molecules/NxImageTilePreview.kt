package com.slothiesmooth.nyx.designlibrary.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.designlibrary.tokens.nxDimensions
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter

private val TilePreviewWidth: Dp = 120.dp

@Composable
fun NxImageTileSample() {
    val dimensions = MaterialTheme.nxDimensions
    Row(
        modifier = Modifier.padding(dimensions.keyline4),
        horizontalArrangement = Arrangement.spacedBy(dimensions.keyline3),
    ) {
        NxImageTile(
            image = null,
            selected = true,
            contentDescription = "Selected cover",
            onClick = {},
            modifier = Modifier.width(TilePreviewWidth),
        )
        NxImageTile(
            image = null,
            selected = false,
            contentDescription = "Cover image",
            modifier = Modifier.width(TilePreviewWidth),
        )
    }
}

@AllThemePreview
@Composable
private fun NxImageTilePaletteAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { NxImageTileSample() }
}
