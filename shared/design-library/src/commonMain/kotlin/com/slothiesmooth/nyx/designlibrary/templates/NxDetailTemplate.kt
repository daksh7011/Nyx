package com.slothiesmooth.nyx.designlibrary.templates

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import com.slothiesmooth.nyx.designlibrary.atoms.NxButton
import com.slothiesmooth.nyx.designlibrary.atoms.NxButtonSize
import com.slothiesmooth.nyx.designlibrary.atoms.NxButtonStyle
import com.slothiesmooth.nyx.designlibrary.atoms.NxChip
import com.slothiesmooth.nyx.designlibrary.atoms.NxIconButton
import com.slothiesmooth.nyx.designlibrary.atoms.NxIconButtonStyle
import com.slothiesmooth.nyx.designlibrary.atoms.NxText
import com.slothiesmooth.nyx.designlibrary.molecules.NxTopBar
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTextStyle
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.designlibrary.tokens.nxColors
import com.slothiesmooth.nyx.designlibrary.tokens.nxDimensions

/**
 * A detail screen scaffold: an [NxTopBar] (with a back affordance and an optional [trailing] actions
 * slot) over a scrolling, padded [content] column.
 */
@Composable
fun NxDetailTemplate(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.nxColors
    val dimensions = MaterialTheme.nxDimensions
    Column(modifier = modifier.fillMaxSize().background(colors.bg)) {
        NxTopBar(title = title, onBack = onBack, trailing = trailing)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(dimensions.keyline4),
            verticalArrangement = Arrangement.spacedBy(dimensions.keyline3),
            content = content,
        )
    }
}

@Composable
fun NxDetailTemplateSample() {
    val colors = MaterialTheme.nxColors
    val dimensions = MaterialTheme.nxDimensions
    // The template is a full-screen scaffold, so each state is bounded to a fixed height and stacked.
    val compactHeight = dimensions.keyline32 + dimensions.keyline16
    val tallHeight = dimensions.keyline32 + dimensions.keyline24 + dimensions.keyline16
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.bgSunken)
            .verticalScroll(rememberScrollState())
            .padding(dimensions.keyline4),
        verticalArrangement = Arrangement.spacedBy(dimensions.keyline4),
    ) {
        DetailStateLabel(text = "No actions · sparse content")
        DetailNoActionsState(height = compactHeight)
        DetailStateLabel(text = "Single action · primary + soft")
        DetailSingleActionState(height = tallHeight)
        DetailStateLabel(text = "Multiple actions · dense content")
        DetailMultipleActionsState(height = tallHeight)
        DetailStateLabel(text = "Long title · truncation")
        DetailLongTitleState(height = compactHeight)
    }
}

@Composable
private fun DetailStateLabel(text: String) {
    NxText(text = text, style = NxTextStyle.Kicker, color = MaterialTheme.nxColors.fgSubtle)
}

@Composable
private fun DetailStateFrame(height: Dp, content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(height)) {
        content()
    }
}

@Composable
private fun DetailNoActionsState(height: Dp) {
    DetailStateFrame(height = height) {
        NxDetailTemplate(title = "Details", onBack = {}) {
            NxText(
                text = "No trailing actions, and a single line of content.",
                style = NxTextStyle.Body,
                color = MaterialTheme.nxColors.fgMuted,
            )
        }
    }
}

@Composable
private fun DetailSingleActionState(height: Dp) {
    DetailStateFrame(height = height) {
        NxDetailTemplate(
            title = "vacation-2026.png",
            onBack = {},
            trailing = {
                NxIconButton(
                    kind = NxIconKind.Share,
                    onClick = {},
                    style = NxIconButtonStyle.Ghost,
                    contentDescription = "Share",
                )
            },
        ) {
            NxText("Saved 2 hours ago", style = NxTextStyle.Caption, color = MaterialTheme.nxColors.fgMuted)
            NxButton(
                text = "Decrypt this",
                onClick = {},
                block = true,
                leadingIcon = NxIconKind.Unlock,
            )
            NxButton(
                text = "Archive",
                onClick = {},
                style = NxButtonStyle.Secondary,
                block = true,
                leadingIcon = NxIconKind.Archive,
            )
        }
    }
}

@Composable
private fun DetailMultipleActionsState(height: Dp) {
    DetailStateFrame(height = height) {
        NxDetailTemplate(
            title = "Encrypted note",
            onBack = {},
            trailing = {
                NxIconButton(
                    kind = NxIconKind.Share,
                    onClick = {},
                    style = NxIconButtonStyle.Ghost,
                    contentDescription = "Share",
                )
                NxIconButton(
                    kind = NxIconKind.Copy,
                    onClick = {},
                    style = NxIconButtonStyle.Ghost,
                    contentDescription = "Copy",
                )
                NxIconButton(
                    kind = NxIconKind.Trash,
                    onClick = {},
                    style = NxIconButtonStyle.Ghost,
                    contentDescription = "Delete",
                )
            },
        ) {
            NxText("Metadata", style = NxTextStyle.Kicker, color = MaterialTheme.nxColors.fgSubtle)
            NxText("Encrypted with AES-256-GCM. Password required to reveal.", style = NxTextStyle.Body)
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.nxDimensions.keyline2)) {
                NxChip(text = "Vault", selected = true, onClick = {})
                NxChip(text = "Shared", selected = false, onClick = {})
            }
            NxButton(text = "Decrypt", onClick = {}, block = true, leadingIcon = NxIconKind.Unlock)
            NxButton(
                text = "Duplicate",
                onClick = {},
                style = NxButtonStyle.Secondary,
                block = true,
                leadingIcon = NxIconKind.Copy,
            )
            NxButton(
                text = "Delete forever",
                onClick = {},
                style = NxButtonStyle.Danger,
                block = true,
                leadingIcon = NxIconKind.Trash,
            )
            NxButton(
                text = "Restore",
                onClick = {},
                style = NxButtonStyle.Ghost,
                block = true,
                enabled = false,
                leadingIcon = NxIconKind.Restore,
            )
        }
    }
}

@Composable
private fun DetailLongTitleState(height: Dp) {
    DetailStateFrame(height = height) {
        NxDetailTemplate(
            title = "a-very-long-encrypted-filename-that-should-truncate-2026.png",
            onBack = {},
            trailing = {
                NxIconButton(
                    kind = NxIconKind.Settings,
                    onClick = {},
                    style = NxIconButtonStyle.Ghost,
                    contentDescription = "Settings",
                )
            },
        ) {
            NxText(
                text = "The top bar title is capped to a single line and ellipsized.",
                style = NxTextStyle.Body,
                color = MaterialTheme.nxColors.fgMuted,
            )
            NxButton(text = "Small action", onClick = {}, size = NxButtonSize.Small)
        }
    }
}

@AllThemePreview
@Composable
private fun NxDetailTemplatePaletteAll(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { NxDetailTemplateSample() }
}
