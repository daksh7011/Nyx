package com.slothiesmooth.nyx.designlibrary.atoms

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slothiesmooth.nyx.designlibrary.tokens.NxColors
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.designlibrary.tokens.NxType
import com.slothiesmooth.nyx.designlibrary.tokens.nxColors
import com.slothiesmooth.nyx.designlibrary.tokens.nxCorners
import com.slothiesmooth.nyx.designlibrary.tokens.nxDimensions
import com.slothiesmooth.nyx.designlibrary.tokens.nxType

enum class NxButtonStyle { Primary, Soft, Ghost, Danger }
enum class NxButtonSize { Regular, Small }

private val ButtonHeightRegular = 48.dp
private val ButtonHeightSmall = 36.dp
private val ButtonPadRegular = 20.dp
private val ButtonPadSmall = 14.dp
private val ButtonTextSizeSmall = 13.sp
private val ButtonIconSize = 16.dp
private val GhostBorderWidth = 1.dp
private const val DISABLED_ALPHA = 0.4f

private fun backgroundFor(style: NxButtonStyle, colors: NxColors): Color = when (style) {
    NxButtonStyle.Primary -> colors.brand
    NxButtonStyle.Soft -> colors.brandSoft
    NxButtonStyle.Ghost -> Color.Transparent
    NxButtonStyle.Danger -> colors.danger
}

private fun foregroundFor(style: NxButtonStyle, colors: NxColors): Color = when (style) {
    NxButtonStyle.Primary -> colors.brandFg
    NxButtonStyle.Soft -> colors.brand
    NxButtonStyle.Ghost -> colors.fg
    NxButtonStyle.Danger -> colors.fgOnBrand
}

private fun borderFor(style: NxButtonStyle, colors: NxColors): BorderStroke? =
    if (style == NxButtonStyle.Ghost) BorderStroke(GhostBorderWidth, colors.borderStrong) else null

private fun textStyleFor(size: NxButtonSize, type: NxType) = if (size == NxButtonSize.Regular) {
    type.bodyStrong
} else {
    type.caption.copy(fontWeight = FontWeight.SemiBold, fontSize = ButtonTextSizeSmall)
}

@Composable
fun NxButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: NxButtonStyle = NxButtonStyle.Primary,
    size: NxButtonSize = NxButtonSize.Regular,
    block: Boolean = false,
    leadingIcon: NxIconKind? = null,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.nxColors
    val background = backgroundFor(style, colors)
    val foreground = foregroundFor(style, colors)
    val border = borderFor(style, colors)
    val height = if (size == NxButtonSize.Regular) ButtonHeightRegular else ButtonHeightSmall
    val horizontalPad = if (size == NxButtonSize.Regular) ButtonPadRegular else ButtonPadSmall
    val textStyle = textStyleFor(size, MaterialTheme.nxType)

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(MaterialTheme.nxCorners.sm),
        color = background,
        contentColor = foreground,
        border = border,
        modifier = modifier
            .let { if (block) it.fillMaxWidth() else it }
            .alpha(if (enabled) 1f else DISABLED_ALPHA),
    ) {
        Row(
            modifier = Modifier.height(height).padding(horizontal = horizontalPad),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                MaterialTheme.nxDimensions.keyline2,
                Alignment.CenterHorizontally,
            ),
        ) {
            if (leadingIcon != null) {
                NxIcon(kind = leadingIcon, tint = foreground, size = ButtonIconSize)
            }
            Text(text = text, style = textStyle, color = foreground)
        }
    }
}
