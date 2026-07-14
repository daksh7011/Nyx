package com.slothiesmooth.nyx.designlibrary.molecules

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.slothiesmooth.nyx.designlibrary.atoms.NxText
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTextStyle
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.designlibrary.tokens.nxColors
import com.slothiesmooth.nyx.designlibrary.tokens.nxCorners
import com.slothiesmooth.nyx.designlibrary.tokens.nxDimensions
import com.slothiesmooth.nyx.designlibrary.tokens.nxElevations

private val CardBorderWidth = 1.dp

@Composable
fun NxCard(
    modifier: Modifier = Modifier,
    variant: NxCardVariant = NxCardVariant.Elevated,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.nxColors
    val shape = RoundedCornerShape(MaterialTheme.nxCorners.lg)
    val background = when (variant) {
        NxCardVariant.Elevated -> colors.bgElev1
        NxCardVariant.Flat -> Color.Transparent
    }
    val border = when (variant) {
        NxCardVariant.Elevated -> BorderStroke(CardBorderWidth, colors.border)
        NxCardVariant.Flat -> BorderStroke(CardBorderWidth, colors.divider)
    }
    val baseModifier = when (variant) {
        NxCardVariant.Elevated -> modifier.shadow(MaterialTheme.nxElevations.level1, shape)
        NxCardVariant.Flat -> modifier
    }
    val contentPadding = MaterialTheme.nxDimensions.keyline4

    if (onClick != null) {
        Surface(onClick = onClick, shape = shape, color = background, border = border, modifier = baseModifier) {
            Column(Modifier.padding(contentPadding)) { content() }
        }
    } else {
        Surface(shape = shape, color = background, border = border, modifier = baseModifier) {
            Column(Modifier.padding(contentPadding)) { content() }
        }
    }
}

private val SampleWidth = 280.dp

@Composable
private fun NxCardSampleEntry(
    variant: NxCardVariant,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
) {
    val subtleColor = MaterialTheme.nxColors.fgSubtle
    NxCard(variant = variant, onClick = onClick) {
        NxText(title, style = NxTextStyle.BodyStrong)
        NxText(subtitle, style = NxTextStyle.Caption, color = subtleColor)
    }
}

@Composable
fun NxCardSample() {
    val dimensions = MaterialTheme.nxDimensions
    Column(
        modifier = Modifier.padding(dimensions.keyline4).width(SampleWidth),
        verticalArrangement = Arrangement.spacedBy(dimensions.keyline3),
    ) {
        NxCardSampleEntry(
            variant = NxCardVariant.Elevated,
            title = "Elevated, static",
            subtitle = "Border, shadow, elevated surface, not tappable",
            onClick = null,
        )
        NxCardSampleEntry(
            variant = NxCardVariant.Elevated,
            title = "Elevated, clickable",
            subtitle = "Whole elevated surface is a tap target",
            onClick = {},
        )
        NxCardSampleEntry(
            variant = NxCardVariant.Flat,
            title = "Flat, static",
            subtitle = "Divider border, no shadow, transparent, not tappable",
            onClick = null,
        )
        NxCardSampleEntry(
            variant = NxCardVariant.Flat,
            title = "Flat, clickable",
            subtitle = "Whole flat surface is a tap target",
            onClick = {},
        )
        NxCard(variant = NxCardVariant.Elevated) {
            NxText("Single-line content", style = NxTextStyle.BodyStrong)
        }
    }
}

@AllThemePreview
@Composable
private fun NxCardPaletteAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { NxCardSample() }
}
