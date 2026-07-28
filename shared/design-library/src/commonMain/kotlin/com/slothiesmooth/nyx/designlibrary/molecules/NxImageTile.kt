package com.slothiesmooth.nyx.designlibrary.molecules

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.slothiesmooth.nyx.designlibrary.atoms.NxIcon
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.designlibrary.tokens.nxColors
import com.slothiesmooth.nyx.designlibrary.tokens.nxCorners
import com.slothiesmooth.nyx.designlibrary.tokens.nxDimensions

private const val TILE_ASPECT_RATIO = 1f
private val TileBorderWidth: Dp = 2.dp
private val TilePlaceholderIconSize: Dp = 32.dp

/**
 * A square image tile rendering an already-decoded [image] (or a placeholder glyph when null),
 * clipped to a rounded shape. A [selected] tile gains a brand-tinted border; a non-null [onClick]
 * makes the whole tile a tap target.
 */
@Composable
fun NxImageTile(
    image: ImageBitmap?,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.nxColors
    val shape = RoundedCornerShape(MaterialTheme.nxCorners.md)
    val border = if (selected) BorderStroke(TileBorderWidth, colors.brand) else null
    val tileModifier = modifier.aspectRatio(TILE_ASPECT_RATIO)
    if (onClick != null) {
        Surface(onClick = onClick, shape = shape, color = colors.bgElev2, border = border, modifier = tileModifier) {
            NxImageTileContent(image = image, contentDescription = contentDescription, placeholderTint = colors.fgFaint)
        }
    } else {
        Surface(shape = shape, color = colors.bgElev2, border = border, modifier = tileModifier) {
            NxImageTileContent(image = image, contentDescription = contentDescription, placeholderTint = colors.fgFaint)
        }
    }
}

@Composable
private fun NxImageTileContent(
    image: ImageBitmap?,
    contentDescription: String?,
    placeholderTint: Color,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            NxIcon(
                kind = NxIconKind.Image,
                tint = placeholderTint,
                size = TilePlaceholderIconSize,
                contentDescription = contentDescription,
            )
        }
    }
}

private const val SAMPLE_IMAGE_PX = 96
private val TilePreviewWidth: Dp = 120.dp

/**
 * Builds a two-tone square bitmap so a filled tile is visually distinct from an empty (placeholder)
 * one in the snapshot. The tones come from the active theme, keeping the fixture free of raw literals.
 */
@Composable
private fun rememberSampleTileImage(primary: Color, secondary: Color): ImageBitmap =
    remember(primary, secondary) {
        val bitmap = ImageBitmap(SAMPLE_IMAGE_PX, SAMPLE_IMAGE_PX)
        val canvas = Canvas(bitmap)
        val edge = SAMPLE_IMAGE_PX.toFloat()
        val mid = edge / 2f
        canvas.drawRect(0f, 0f, edge, edge, Paint().apply { color = primary })
        canvas.drawRect(mid, mid, edge, edge, Paint().apply { color = secondary })
        canvas.drawRect(0f, 0f, mid, mid, Paint().apply { color = secondary })
        bitmap
    }

@Composable
fun NxImageTileSample() {
    val dimensions = MaterialTheme.nxDimensions
    val colors = MaterialTheme.nxColors
    val sampleImage = rememberSampleTileImage(primary = colors.accentViolet, secondary = colors.accentCyan)
    Column(
        modifier = Modifier.padding(dimensions.keyline4),
        verticalArrangement = Arrangement.spacedBy(dimensions.keyline3),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(dimensions.keyline3)) {
            NxImageTile(
                image = sampleImage,
                selected = false,
                contentDescription = "Cover image, unselected",
                modifier = Modifier.width(TilePreviewWidth),
            )
            NxImageTile(
                image = sampleImage,
                selected = true,
                contentDescription = "Cover image, selected",
                onClick = {},
                modifier = Modifier.width(TilePreviewWidth),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(dimensions.keyline3)) {
            NxImageTile(
                image = null,
                selected = false,
                contentDescription = "Empty cover, unselected",
                modifier = Modifier.width(TilePreviewWidth),
            )
            NxImageTile(
                image = null,
                selected = true,
                contentDescription = "Empty cover, selected",
                onClick = {},
                modifier = Modifier.width(TilePreviewWidth),
            )
        }
    }
}

@AllThemePreview
@Composable
private fun NxImageTilePaletteAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { NxImageTileSample() }
}
