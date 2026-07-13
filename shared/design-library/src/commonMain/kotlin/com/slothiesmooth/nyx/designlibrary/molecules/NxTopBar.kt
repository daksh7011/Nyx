package com.slothiesmooth.nyx.designlibrary.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.slothiesmooth.nyx.designlibrary.atoms.NxIconButton
import com.slothiesmooth.nyx.designlibrary.atoms.NxIconButtonStyle
import com.slothiesmooth.nyx.designlibrary.atoms.NxText
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.designlibrary.tokens.NxTextStyle
import com.slothiesmooth.nyx.designlibrary.tokens.nxColors
import com.slothiesmooth.nyx.designlibrary.tokens.nxDimensions

private const val BACK_CONTENT_DESCRIPTION = "Back"

/**
 * A screen top bar: an optional [onBack] back affordance, a [title] with an optional [subtitle]
 * underneath, and a right-aligned [trailing] actions slot. Stateless — every affordance is supplied
 * by the caller.
 */
@Composable
fun NxTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val colors = MaterialTheme.nxColors
    val dimensions = MaterialTheme.nxDimensions
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dimensions.keyline4, vertical = dimensions.keyline3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensions.keyline2),
    ) {
        if (onBack != null) {
            NxIconButton(
                kind = NxIconKind.ChevronLeft,
                onClick = onBack,
                style = NxIconButtonStyle.Ghost,
                contentDescription = BACK_CONTENT_DESCRIPTION,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            NxText(text = title, style = NxTextStyle.Title, color = colors.fg, maxLines = 1)
            if (subtitle != null) {
                NxText(text = subtitle, style = NxTextStyle.Caption, color = colors.fgMuted, maxLines = 1)
            }
        }
        trailing()
    }
}
