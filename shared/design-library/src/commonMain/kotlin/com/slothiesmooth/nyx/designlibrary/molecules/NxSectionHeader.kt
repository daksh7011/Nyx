package com.slothiesmooth.nyx.designlibrary.molecules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.slothiesmooth.nyx.designlibrary.atoms.NxText
import com.slothiesmooth.nyx.designlibrary.tokens.NxTextStyle
import com.slothiesmooth.nyx.designlibrary.tokens.nxColors
import com.slothiesmooth.nyx.designlibrary.tokens.nxDimensions

/**
 * A list/section header: a weighted [title] with an optional right-aligned action rendered as a
 * brand-tinted tappable label ([actionText] + [onAction], e.g. "See all").
 */
@Composable
fun NxSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.nxColors
    val dimensions = MaterialTheme.nxDimensions
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensions.keyline2),
    ) {
        NxText(text = title, style = NxTextStyle.Subhead, color = colors.fg, modifier = Modifier.weight(1f))
        if (actionText != null && onAction != null) {
            NxText(
                text = actionText,
                style = NxTextStyle.BodyStrong,
                color = colors.brand,
                modifier = Modifier.clickable(onClick = onAction),
            )
        }
    }
}
