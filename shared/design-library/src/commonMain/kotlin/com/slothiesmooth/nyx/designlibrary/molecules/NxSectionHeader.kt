package com.slothiesmooth.nyx.designlibrary.molecules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.slothiesmooth.nyx.designlibrary.atoms.NxText
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTextStyle
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
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

@Composable
fun NxSectionHeaderSample() {
    val dimensions = MaterialTheme.nxDimensions
    Column(
        modifier = Modifier.padding(dimensions.keyline4),
        verticalArrangement = Arrangement.spacedBy(dimensions.keyline3),
    ) {
        // Title only: no optional action slot.
        NxSectionHeader(title = "Archived")
        // Title with a right-aligned brand action.
        NxSectionHeader(title = "Recent", actionText = "See all", onAction = {})
        // Short title with action: the weighted title collapses toward the action.
        NxSectionHeader(title = "Pinned", actionText = "Manage", onAction = {})
        // Long title with action: the weighted title fills, pushing the action to the edge.
        NxSectionHeader(
            title = "Recently added companions and their evolutions",
            actionText = "See all",
            onAction = {},
        )
        // Long title only: fills the full width without an action slot.
        NxSectionHeader(title = "Everything you have discovered so far in this region")
    }
}

@AllThemePreview
@Composable
private fun NxSectionHeaderPaletteAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { NxSectionHeaderSample() }
}
