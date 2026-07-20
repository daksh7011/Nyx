package com.slothiesmooth.nyx.designlibrary.atoms

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.designlibrary.tokens.nxColors
import com.slothiesmooth.nyx.designlibrary.tokens.nxDimensions

private val IconButtonSize = 36.dp
private val IconButtonIconSize = 20.dp
private val IconButtonBorderWidth = 1.dp

@Composable
fun NxIconButton(
    kind: NxIconKind,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: NxIconButtonStyle = NxIconButtonStyle.Outline,
    contentDescription: String? = null,
) {
    val colors = MaterialTheme.nxColors
    val background = if (style == NxIconButtonStyle.Filled) colors.brand else Color.Transparent
    val tint = if (style == NxIconButtonStyle.Filled) colors.brandFg else colors.fg
    val border =
        if (style == NxIconButtonStyle.Outline) BorderStroke(IconButtonBorderWidth, colors.border) else null
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = background,
        border = border,
        modifier = modifier.size(IconButtonSize),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            NxIcon(
                kind = kind,
                tint = tint,
                size = IconButtonIconSize,
                contentDescription = contentDescription,
            )
        }
    }
}

@Composable
fun NxIconButtonSample() {
    Row(
        modifier = Modifier.padding(MaterialTheme.nxDimensions.keyline4),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.nxDimensions.keyline3),
    ) {
        NxIconButton(kind = NxIconKind.Plus, onClick = {}, style = NxIconButtonStyle.Outline)
        NxIconButton(kind = NxIconKind.Share, onClick = {}, style = NxIconButtonStyle.Ghost)
        NxIconButton(kind = NxIconKind.Camera, onClick = {}, style = NxIconButtonStyle.Filled)
    }
}

@AllThemePreview
@Composable
private fun NxIconButtonPaletteAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { NxIconButtonSample() }
}
