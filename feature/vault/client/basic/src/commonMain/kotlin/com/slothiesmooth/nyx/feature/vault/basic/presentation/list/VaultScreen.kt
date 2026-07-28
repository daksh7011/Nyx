package com.slothiesmooth.nyx.feature.vault.basic.presentation.list

import androidx.compose.runtime.Composable
import com.slothiesmooth.nyx.feature.common.koin.koinFeatureViewModel
import com.slothiesmooth.nyx.shared.data.id.StegoImageId

/** Resolves [VaultViewModel] from the isolated graph and binds it to the stateless [VaultContent]. */
@Composable
fun VaultScreen(
    onOpenDetail: (StegoImageId) -> Unit,
    onOpenEncrypt: () -> Unit,
) {
    val viewModel = koinFeatureViewModel<VaultViewModel>()
    VaultContent(
        state = viewModel.state,
        onOpenDetail = onOpenDetail,
        onOpenEncrypt = onOpenEncrypt,
        onToggleArchived = viewModel::toggleArchived,
        onOpenArchivedDetail = onOpenDetail,
    )
}
