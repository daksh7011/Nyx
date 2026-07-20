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
import com.slothiesmooth.nyx.feature.settings.basic.resources.Res
import com.slothiesmooth.nyx.feature.settings.basic.resources.settings_appearance
import com.slothiesmooth.nyx.feature.settings.basic.resources.settings_cancel
import com.slothiesmooth.nyx.feature.settings.basic.resources.settings_licenses_title
import com.slothiesmooth.nyx.feature.settings.basic.resources.settings_source_code
import com.slothiesmooth.nyx.feature.settings.basic.resources.settings_title
import com.slothiesmooth.nyx.feature.settings.basic.resources.settings_wipe_confirm_action
import com.slothiesmooth.nyx.feature.settings.basic.resources.settings_wipe_confirm_message
import com.slothiesmooth.nyx.feature.settings.basic.resources.settings_wipe_confirm_title
import com.slothiesmooth.nyx.feature.settings.basic.resources.settings_wipe_vault
import com.slothiesmooth.nyx.feature.settings.basic.resources.settings_wiping_progress
import com.slothiesmooth.nyx.shared.presentation.state.UiEvent
import com.slothiesmooth.nyx.shared.presentation.state.UiState
import com.slothiesmooth.nyx.shared.presentation.text.UiText
import com.slothiesmooth.nyx.shared.presentation.util.asString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.jetbrains.compose.resources.stringResource

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
            NxTopBar(title = stringResource(Res.string.settings_title))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(dimensions.keyline4),
                verticalArrangement = Arrangement.spacedBy(dimensions.keyline3),
            ) {
                NxText(
                    text = state.versionLabel.asString(),
                    style = NxTextStyle.Caption,
                    color = MaterialTheme.nxColors.fgMuted,
                )
                SettingsRow(
                    icon = NxIconKind.Palette,
                    label = stringResource(Res.string.settings_appearance),
                    onClick = onOpenTheme,
                )
                SettingsRow(
                    icon = NxIconKind.Info,
                    label = stringResource(Res.string.settings_licenses_title),
                    onClick = onOpenLicenses,
                )
                SettingsRow(
                    icon = NxIconKind.Share,
                    label = stringResource(Res.string.settings_source_code),
                    onClick = onOpenRepo,
                )
                NxButton(
                    text = stringResource(Res.string.settings_wipe_vault),
                    onClick = onRequestWipe,
                    style = NxButtonStyle.Danger,
                    block = true,
                    leadingIcon = NxIconKind.Trash,
                )
            }
        }
        if (state.showWipeConfirm) {
            WipeConfirmDialog(onConfirmWipe = onConfirmWipe, onCancelWipe = onCancelWipe)
        }
        if (state.isWiping) {
            NxProgressOverlay(
                label = stringResource(Res.string.settings_wiping_progress),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun WipeConfirmDialog(onConfirmWipe: () -> Unit, onCancelWipe: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancelWipe,
        title = {
            NxText(text = stringResource(Res.string.settings_wipe_confirm_title), style = NxTextStyle.Subhead)
        },
        text = {
            NxText(text = stringResource(Res.string.settings_wipe_confirm_message), style = NxTextStyle.Body)
        },
        confirmButton = {
            NxButton(
                text = stringResource(Res.string.settings_wipe_confirm_action),
                onClick = onConfirmWipe,
                style = NxButtonStyle.Danger,
            )
        },
        dismissButton = {
            NxButton(
                text = stringResource(Res.string.settings_cancel),
                onClick = onCancelWipe,
                style = NxButtonStyle.Ghost,
            )
        },
    )
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
    override val versionLabel: UiText,
    override val showWipeConfirm: Boolean,
    override val isWiping: Boolean,
) : SettingsState {
    override val uiState: UiState = UiState.Ready
    override val uiEvent: Flow<UiEvent> = emptyFlow()
}

private val previewVersionLabel = UiText.raw("Nyx 1.0.0 · Android")

@AllThemePreview
@Composable
private fun SettingsContentPreview(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { SettingsContent(PreviewSettingsState(previewVersionLabel, false, false)) }
}

@AllThemePreview
@Composable
private fun SettingsWipeConfirmPreview(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { SettingsContent(PreviewSettingsState(previewVersionLabel, true, false)) }
}

@AllThemePreview
@Composable
private fun SettingsWipingPreview(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { SettingsContent(PreviewSettingsState(previewVersionLabel, false, true)) }
}
