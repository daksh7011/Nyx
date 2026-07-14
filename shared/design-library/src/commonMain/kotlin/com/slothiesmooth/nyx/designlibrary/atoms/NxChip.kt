package com.slothiesmooth.nyx.designlibrary.atoms

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.designlibrary.tokens.nxColors
import com.slothiesmooth.nyx.designlibrary.tokens.nxCorners
import com.slothiesmooth.nyx.designlibrary.tokens.nxDimensions
import com.slothiesmooth.nyx.designlibrary.tokens.nxType

private val ChipHorizontalPadding = 14.dp
private val ChipTextSize = 13.sp
private val ChipBorderWidth = 1.dp

@Composable
fun NxChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.nxColors
    val type = MaterialTheme.nxType
    val background = if (selected) colors.brand else Color.Transparent
    val foreground = if (selected) colors.brandFg else colors.fg
    val border = if (selected) null else BorderStroke(ChipBorderWidth, colors.borderStrong)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(MaterialTheme.nxCorners.pill),
        color = background,
        border = border,
        modifier = modifier,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = ChipHorizontalPadding,
                vertical = MaterialTheme.nxDimensions.keyline2,
            ),
            color = foreground,
            style = type.caption.copy(fontWeight = FontWeight.Medium, fontSize = ChipTextSize),
        )
    }
}

@Composable
fun NxChipSample() {
    Row(
        modifier = Modifier.padding(MaterialTheme.nxDimensions.keyline4),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.nxDimensions.keyline2),
    ) {
        NxChip(text = "Selected", selected = true, onClick = {})
        NxChip(text = "Unselected", selected = false, onClick = {})
    }
}

@AllThemePreview
@Composable
private fun NxChipPaletteAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { NxChipSample() }
}
