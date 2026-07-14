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
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.slothiesmooth.nyx.designlibrary.atoms.NxIconButton
import com.slothiesmooth.nyx.designlibrary.atoms.NxIconButtonStyle
import com.slothiesmooth.nyx.designlibrary.atoms.NxText
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTextStyle
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
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

private const val SAMPLE_LONG_TITLE =
    "Quarterly encrypted archive of every field expedition photo"
private const val SAMPLE_LONG_SUBTITLE =
    "Synced across all devices, awaiting the final passphrase confirmation before upload"

@Composable
private fun SampleShareAction() {
    NxIconButton(
        kind = NxIconKind.Share,
        onClick = {},
        style = NxIconButtonStyle.Ghost,
        contentDescription = "Share",
    )
}

@Composable
private fun SampleTwoActions() {
    NxIconButton(
        kind = NxIconKind.Eye,
        onClick = {},
        style = NxIconButtonStyle.Ghost,
        contentDescription = "Reveal",
    )
    NxIconButton(
        kind = NxIconKind.Plus,
        onClick = {},
        style = NxIconButtonStyle.Ghost,
        contentDescription = "Add",
    )
}

@Composable
fun NxTopBarSample() {
    val dimensions = MaterialTheme.nxDimensions
    Column(verticalArrangement = Arrangement.spacedBy(dimensions.keyline4)) {
        // Title only: no back, no subtitle, no trailing.
        NxTopBar(title = "Vault")
        // Title with back affordance only.
        NxTopBar(title = "Settings", onBack = {})
        // Title with subtitle, no back, no trailing.
        NxTopBar(title = "Overview", subtitle = "12 items")
        // Title with a single trailing action, no back, no subtitle.
        NxTopBar(title = "Photos", trailing = { SampleShareAction() })
        // Title with back and subtitle, no trailing.
        NxTopBar(title = "Encrypt", subtitle = "Step 2 of 3", onBack = {})
        // Title with back and a trailing action, no subtitle.
        NxTopBar(title = "Album", onBack = {}, trailing = { SampleShareAction() })
        // Title with subtitle and a trailing action, no back.
        NxTopBar(
            title = "Gallery",
            subtitle = "Updated just now",
            trailing = { SampleShareAction() },
        )
        // All slots populated: back, subtitle, and a single trailing action.
        NxTopBar(
            title = "Encrypt",
            subtitle = "Step 2 of 3",
            onBack = {},
            trailing = { SampleShareAction() },
        )
        // All slots with multiple trailing actions.
        NxTopBar(
            title = "Collection",
            subtitle = "48 photos",
            onBack = {},
            trailing = { SampleTwoActions() },
        )
        // Long title truncates to a single line even with back and trailing present.
        NxTopBar(
            title = SAMPLE_LONG_TITLE,
            onBack = {},
            trailing = { SampleShareAction() },
        )
        // Long subtitle truncates to a single line.
        NxTopBar(
            title = "Archive",
            subtitle = SAMPLE_LONG_SUBTITLE,
            onBack = {},
        )
    }
}

@AllThemePreview
@Composable
private fun NxTopBarPaletteAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { NxTopBarSample() }
}
