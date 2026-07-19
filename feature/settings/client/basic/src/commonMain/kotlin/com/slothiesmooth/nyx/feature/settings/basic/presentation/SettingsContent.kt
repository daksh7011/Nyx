package com.slothiesmooth.nyx.feature.settings.basic.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.slothiesmooth.nyx.designlibrary.atoms.NxButton
import com.slothiesmooth.nyx.designlibrary.atoms.NxButtonStyle
import com.slothiesmooth.nyx.designlibrary.atoms.NxIcon
import com.slothiesmooth.nyx.designlibrary.atoms.NxText
import com.slothiesmooth.nyx.designlibrary.molecules.NxCard
import com.slothiesmooth.nyx.designlibrary.molecules.NxCardVariant
import com.slothiesmooth.nyx.designlibrary.molecules.NxProgressOverlay
import com.slothiesmooth.nyx.designlibrary.molecules.NxTopBar
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTextStyle
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.designlibrary.tokens.nxColors
import com.slothiesmooth.nyx.designlibrary.tokens.nxDimensions
import com.slothiesmooth.nyx.shared.presentation.state.UiEvent
import com.slothiesmooth.nyx.shared.presentation.state.UiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Stateless settings body: about label, appearance/licenses/source-code rows, the danger-zone wipe
 * button, its confirm dialog, and a blocking spinner while the wipe runs. Every value comes from
 * [state]; the composable performs no formatting or mapping.
 */
@Composable
fun SettingsContent(
    state: SettingsState,
    onOpenTheme: () -> Unit = {},
    onOpenLicenses: () -> Unit = {},
    onOpenRepo: () -> Unit = {},
    onRequestWipe: () -> Unit = {},
    onConfirmWipe: () -> Unit = {},
    onCancelWipe: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val dimensions = MaterialTheme.nxDimensions
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            NxTopBar(title = "Settings")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(dimensions.keyline4),
                verticalArrangement = Arrangement.spacedBy(dimensions.keyline3),
            ) {
                NxText(text = state.versionLabel, style = NxTextStyle.Caption, color = MaterialTheme.nxColors.fgMuted)
                SettingsRow(icon = NxIconKind.Palette, label = "Appearance", onClick = onOpenTheme)
                SettingsRow(icon = NxIconKind.Info, label = "Open-source licenses", onClick = onOpenLicenses)
                SettingsRow(icon = NxIconKind.Share, label = "Source code", onClick = onOpenRepo)
                NxButton(
                    text = "Wipe vault",
                    onClick = onRequestWipe,
                    style = NxButtonStyle.Danger,
                    block = true,
                    leadingIcon = NxIconKind.Trash,
                )
            }
        }
        if (state.showWipeConfirm) {
            AlertDialog(
                onDismissRequest = onCancelWipe,
                title = { NxText(text = "Wipe vault?", style = NxTextStyle.Subhead) },
                text = {
                    NxText(
                        text = "This permanently deletes every stored image and its hidden message. " +
                            "This cannot be undone.",
                        style = NxTextStyle.Body,
                    )
                },
                confirmButton = { NxButton(text = "Wipe", onClick = onConfirmWipe, style = NxButtonStyle.Danger) },
                dismissButton = { NxButton(text = "Cancel", onClick = onCancelWipe, style = NxButtonStyle.Ghost) },
            )
        }
        if (state.isWiping) NxProgressOverlay(label = "Wiping vault", modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun SettingsRow(icon: NxIconKind, label: String, onClick: () -> Unit) {
    val dimensions = MaterialTheme.nxDimensions
    NxCard(variant = NxCardVariant.Elevated, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(dimensions.keyline3),
            horizontalArrangement = Arrangement.spacedBy(dimensions.keyline3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NxIcon(kind = icon, tint = MaterialTheme.nxColors.brand)
            NxText(text = label, style = NxTextStyle.Body)
        }
    }
}

private class PreviewSettingsState(
    override val versionLabel: String,
    override val showWipeConfirm: Boolean,
    override val isWiping: Boolean,
) : SettingsState {
    override val uiState: UiState = UiState.Ready
    override val uiEvent: Flow<UiEvent> = emptyFlow()
}

@AllThemePreview
@Composable
private fun SettingsContentPreview(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { SettingsContent(PreviewSettingsState("Nyx 1.0.0 · Android", false, false)) }
}

@AllThemePreview
@Composable
private fun SettingsWipeConfirmPreview(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { SettingsContent(PreviewSettingsState("Nyx 1.0.0 · Android", true, false)) }
}

@AllThemePreview
@Composable
private fun SettingsWipingPreview(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { SettingsContent(PreviewSettingsState("Nyx 1.0.0 · Android", false, true)) }
}
