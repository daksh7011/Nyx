package com.slothiesmooth.nyx.designlibrary.molecules

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
