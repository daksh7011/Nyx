package com.slothiesmooth.nyx.designlibrary.atoms

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.designlibrary.tokens.nxColors

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
