package com.slothiesmooth.nyx.designlibrary.molecules

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.slothiesmooth.nyx.designlibrary.atoms.NxIcon
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.designlibrary.tokens.nxColors
import com.slothiesmooth.nyx.designlibrary.tokens.nxCorners

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
