package com.slothiesmooth.nyx.feature.vault.basic.presentation.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.slothiesmooth.nyx.designlibrary.atoms.NxIconButton
import com.slothiesmooth.nyx.designlibrary.atoms.NxIconButtonStyle
import com.slothiesmooth.nyx.designlibrary.molecules.NxEmptyState
import com.slothiesmooth.nyx.designlibrary.molecules.NxFab
import com.slothiesmooth.nyx.designlibrary.molecules.NxImageTile
import com.slothiesmooth.nyx.designlibrary.molecules.NxProgressOverlay
import com.slothiesmooth.nyx.designlibrary.molecules.NxSectionHeader
import com.slothiesmooth.nyx.designlibrary.molecules.NxTopBar
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.designlibrary.tokens.nxColors
import com.slothiesmooth.nyx.designlibrary.tokens.nxDimensions
import com.slothiesmooth.nyx.shared.data.id.StegoImageId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

private const val GRID_COLUMNS = 2

/** Stateless vault grid: top bar with an archived toggle, empty state, image grid, and encrypt FAB. */
@Composable
fun VaultContent(
    state: VaultState,
    onOpenDetail: (StegoImageId) -> Unit = {},
    onOpenEncrypt: () -> Unit = {},
    onToggleArchived: () -> Unit = {},
    onOpenArchivedDetail: (StegoImageId) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val showEmptyState = state.active.isEmpty() && !state.showArchived
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.nxColors.bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            NxTopBar(
                title = "Vault",
                trailing = {
                    NxIconButton(
                        kind = if (state.showArchived) NxIconKind.Eye else NxIconKind.Archive,
                        onClick = onToggleArchived,
                        style = NxIconButtonStyle.Ghost,
                        contentDescription = "Toggle archived",
                    )
                },
            )
            when {
                state.isLoading -> NxProgressOverlay(label = "Loading vault", modifier = Modifier.fillMaxSize())
                showEmptyState -> NxEmptyState(
                    icon = NxIconKind.Vault,
                    title = "Your vault is empty",
                    body = "Hide an encrypted message inside an image to get started.",
                    ctaText = "Encrypt a message",
                    onCta = onOpenEncrypt,
                    modifier = Modifier.fillMaxSize(),
                )
                else -> VaultGrid(
                    active = state.active,
                    archived = state.archived,
                    showArchived = state.showArchived,
                    onOpenDetail = onOpenDetail,
                    onOpenArchivedDetail = onOpenArchivedDetail,
                )
            }
        }
        if (!state.isLoading && !showEmptyState) {
            NxFab(
                icon = NxIconKind.Plus,
                onClick = onOpenEncrypt,
                contentDescription = "Encrypt a message",
                modifier = Modifier.align(Alignment.BottomEnd).padding(MaterialTheme.nxDimensions.keyline5),
            )
        }
    }
}

@Composable
private fun VaultGrid(
    active: ImmutableList<VaultImageUi>,
    archived: ImmutableList<VaultImageUi>,
    showArchived: Boolean,
    onOpenDetail: (StegoImageId) -> Unit,
    onOpenArchivedDetail: (StegoImageId) -> Unit,
) {
    val dimensions = MaterialTheme.nxDimensions
    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMNS),
        contentPadding = PaddingValues(dimensions.keyline4),
        horizontalArrangement = Arrangement.spacedBy(dimensions.keyline3),
        verticalArrangement = Arrangement.spacedBy(dimensions.keyline3),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(active, key = { it.id.value }) { row ->
            NxImageTile(
                image = row.thumbnail,
                contentDescription = row.name,
                onClick = { onOpenDetail(row.id) },
            )
        }
        if (showArchived && archived.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { NxSectionHeader(title = "Archived") }
            items(archived, key = { "archived-${it.id.value}" }) { row ->
                NxImageTile(
                    image = row.thumbnail,
                    contentDescription = row.name,
                    onClick = { onOpenArchivedDetail(row.id) },
                )
            }
        }
    }
}

private fun sampleRows(count: Int): ImmutableList<VaultImageUi> =
    (1..count).map { VaultImageUi(StegoImageId("id-$it"), "nyx-000$it.png", "Jul 13, 2026", null) }.toImmutableList()

private fun previewState(
    active: ImmutableList<VaultImageUi> = persistentListOf(),
    archived: ImmutableList<VaultImageUi> = persistentListOf(),
    showArchived: Boolean = false,
    isLoading: Boolean = false,
): VaultState = VaultMutableState().apply {
    this.active = active
    this.archived = archived
    this.showArchived = showArchived
    this.isLoading = isLoading
}

@AllThemePreview
@Composable
private fun VaultLoadingPreview(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { VaultContent(previewState(isLoading = true)) }
}

@AllThemePreview
@Composable
private fun VaultEmptyPreview(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { VaultContent(previewState()) }
}

@AllThemePreview
@Composable
private fun VaultContentPreview(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) {
        VaultContent(
            previewState(active = sampleRows(count = 4), archived = sampleRows(count = 2), showArchived = true),
        )
    }
}
