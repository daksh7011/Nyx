package com.slothiesmooth.nyx.designlibrary.atoms

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxColors
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
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
    NxButtonStyle.Soft -> colors.bgElev2
    NxButtonStyle.Ghost -> Color.Transparent
    NxButtonStyle.Danger -> colors.danger
}

private fun foregroundFor(style: NxButtonStyle, colors: NxColors): Color = when (style) {
    NxButtonStyle.Primary -> colors.brandFg
    NxButtonStyle.Soft -> colors.fg
    NxButtonStyle.Ghost -> colors.fg
    NxButtonStyle.Danger -> colors.fgOnBrand
}

// Filled primary/danger, a raised tonal Soft (visible surface + hairline), an outlined Ghost.
private fun borderFor(style: NxButtonStyle, colors: NxColors): BorderStroke? = when (style) {
    NxButtonStyle.Soft -> BorderStroke(GhostBorderWidth, colors.border)
    NxButtonStyle.Ghost -> BorderStroke(GhostBorderWidth, colors.borderStrong)
    else -> null
}

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

private val SampleWidth = 400.dp

@Composable
fun NxButtonSample() {
    Column(
        modifier = Modifier.padding(MaterialTheme.nxDimensions.keyline4).width(SampleWidth),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.nxDimensions.keyline3),
    ) {
        SampleSectionLabel(text = "Regular")
        NxButtonStyleRow(size = NxButtonSize.Regular, enabled = true, withIcon = false)

        SampleSectionLabel(text = "Small")
        NxButtonStyleRow(size = NxButtonSize.Small, enabled = true, withIcon = false)

        SampleSectionLabel(text = "Regular with leading icon")
        NxButtonStyleRow(size = NxButtonSize.Regular, enabled = true, withIcon = true)

        SampleSectionLabel(text = "Small with leading icon")
        NxButtonStyleRow(size = NxButtonSize.Small, enabled = true, withIcon = true)

        SampleSectionLabel(text = "Disabled")
        NxButtonStyleRow(size = NxButtonSize.Regular, enabled = false, withIcon = true)
        NxButtonStyleRow(size = NxButtonSize.Small, enabled = false, withIcon = false)

        SampleSectionLabel(text = "Block")
        NxButtonBlockSection()
    }
}

@Composable
private fun SampleSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.nxType.caption,
        color = MaterialTheme.nxColors.fgMuted,
    )
}

@Composable
private fun NxButtonStyleRow(size: NxButtonSize, enabled: Boolean, withIcon: Boolean) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.nxDimensions.keyline2),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.nxDimensions.keyline2),
    ) {
        NxButton(
            text = "Primary",
            onClick = {},
            style = NxButtonStyle.Primary,
            size = size,
            enabled = enabled,
            leadingIcon = if (withIcon) NxIconKind.Lock else null,
        )
        NxButton(
            text = "Soft",
            onClick = {},
            style = NxButtonStyle.Soft,
            size = size,
            enabled = enabled,
            leadingIcon = if (withIcon) NxIconKind.Eye else null,
        )
        NxButton(
            text = "Ghost",
            onClick = {},
            style = NxButtonStyle.Ghost,
            size = size,
            enabled = enabled,
            leadingIcon = if (withIcon) NxIconKind.Settings else null,
        )
        NxButton(
            text = "Danger",
            onClick = {},
            style = NxButtonStyle.Danger,
            size = size,
            enabled = enabled,
            leadingIcon = if (withIcon) NxIconKind.Trash else null,
        )
    }
}

@Composable
private fun NxButtonBlockSection() {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.nxDimensions.keyline2)) {
        NxButton(
            text = "Encrypt message",
            onClick = {},
            style = NxButtonStyle.Primary,
            block = true,
            leadingIcon = NxIconKind.Lock,
        )
        NxButton(
            text = "Unlock vault",
            onClick = {},
            style = NxButtonStyle.Ghost,
            block = true,
            leadingIcon = NxIconKind.Unlock,
        )
        NxButton(
            text = "Wipe vault",
            onClick = {},
            style = NxButtonStyle.Danger,
            block = true,
            leadingIcon = NxIconKind.Trash,
        )
        NxButton(
            text = "Save draft",
            onClick = {},
            style = NxButtonStyle.Soft,
            block = true,
        )
    }
}

@AllThemePreview
@Composable
private fun NxButtonPaletteAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { NxButtonSample() }
}
