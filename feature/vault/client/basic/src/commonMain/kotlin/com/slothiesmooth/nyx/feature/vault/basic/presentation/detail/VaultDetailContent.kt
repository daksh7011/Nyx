package com.slothiesmooth.nyx.feature.vault.basic.presentation.detail

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.slothiesmooth.nyx.designlibrary.atoms.NxButton
import com.slothiesmooth.nyx.designlibrary.atoms.NxButtonStyle
import com.slothiesmooth.nyx.designlibrary.atoms.NxIconButton
import com.slothiesmooth.nyx.designlibrary.atoms.NxText
import com.slothiesmooth.nyx.designlibrary.molecules.NxImageTile
import com.slothiesmooth.nyx.designlibrary.molecules.NxProgressOverlay
import com.slothiesmooth.nyx.designlibrary.templates.NxDetailTemplate
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTextStyle
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.designlibrary.tokens.nxColors
import com.slothiesmooth.nyx.feature.vault.basic.resources.Res
import com.slothiesmooth.nyx.feature.vault.basic.resources.vault_archive
import com.slothiesmooth.nyx.feature.vault.basic.resources.vault_decrypt_this
import com.slothiesmooth.nyx.feature.vault.basic.resources.vault_delete
import com.slothiesmooth.nyx.feature.vault.basic.resources.vault_detail_loading
import com.slothiesmooth.nyx.feature.vault.basic.resources.vault_detail_untitled
import com.slothiesmooth.nyx.feature.vault.basic.resources.vault_restore
import com.slothiesmooth.nyx.feature.vault.basic.resources.vault_share
import com.slothiesmooth.nyx.shared.presentation.text.UiText
import com.slothiesmooth.nyx.shared.presentation.util.asString
import org.jetbrains.compose.resources.stringResource

/** Stateless detail view: preview tile, created date, and decrypt/archive-or-restore/delete actions. */
@Composable
fun VaultDetailContent(
    state: VaultDetailState,
    onBack: () -> Unit = {},
    onArchive: () -> Unit = {},
    onRestore: () -> Unit = {},
    onDelete: () -> Unit = {},
    onShare: () -> Unit = {},
    onDecrypt: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val untitled = stringResource(Res.string.vault_detail_untitled)
    NxDetailTemplate(
        title = state.name.ifBlank { untitled },
        onBack = onBack,
        modifier = modifier,
        trailing = {
            NxIconButton(
                kind = NxIconKind.Share,
                onClick = onShare,
                contentDescription = stringResource(Res.string.vault_share),
            )
        },
    ) {
        if (state.isLoading) {
            NxProgressOverlay(label = stringResource(Res.string.vault_detail_loading))
        } else {
            DetailBody(
                state = state,
                onDecrypt = onDecrypt,
                onRestore = onRestore,
                onArchive = onArchive,
                onDelete = onDelete,
            )
        }
    }
}

@Composable
private fun DetailBody(
    state: VaultDetailState,
    onDecrypt: () -> Unit,
    onRestore: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    NxImageTile(
        image = state.thumbnail,
        contentDescription = state.name,
        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
    )
    NxText(
        text = state.createdLabel.asString(),
        style = NxTextStyle.Caption,
        color = MaterialTheme.nxColors.fgMuted,
    )
    NxButton(
        text = stringResource(Res.string.vault_decrypt_this),
        onClick = onDecrypt,
        style = NxButtonStyle.Primary,
        block = true,
        leadingIcon = NxIconKind.Unlock,
    )
    if (state.isArchived) {
        NxButton(
            text = stringResource(Res.string.vault_restore),
            onClick = onRestore,
            style = NxButtonStyle.Secondary,
            block = true,
            leadingIcon = NxIconKind.Restore,
        )
    } else {
        NxButton(
            text = stringResource(Res.string.vault_archive),
            onClick = onArchive,
            style = NxButtonStyle.Secondary,
            block = true,
            leadingIcon = NxIconKind.Archive,
        )
    }
    NxButton(
        text = stringResource(Res.string.vault_delete),
        onClick = onDelete,
        style = NxButtonStyle.Danger,
        block = true,
        leadingIcon = NxIconKind.Trash,
    )
}

private fun previewState(
    name: String,
    createdLabel: UiText,
    isArchived: Boolean = false,
    isLoading: Boolean = false,
): VaultDetailState = VaultDetailMutableState().apply {
    this.name = name
    this.createdLabel = createdLabel
    this.isArchived = isArchived
    this.isLoading = isLoading
}

@AllThemePreview
@Composable
private fun VaultDetailLoadingPreview(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) {
        VaultDetailContent(previewState(name = "nyx-0001.png", createdLabel = UiText.raw(""), isLoading = true))
    }
}

@AllThemePreview
@Composable
private fun VaultDetailContentPreview(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) {
        VaultDetailContent(previewState(name = "nyx-0001.png", createdLabel = UiText.raw("Jul 13, 2026")))
    }
}

@AllThemePreview
@Composable
private fun VaultDetailArchivedPreview(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) {
        VaultDetailContent(
            previewState(
                name = "nyx-0002.png",
                createdLabel = UiText.raw("Jul 10, 2026"),
                isArchived = true,
            ),
        )
    }
}
