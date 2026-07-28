package com.slothiesmooth.nyx.feature.vault.basic.presentation.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.slothiesmooth.nyx.feature.common.koin.koinFeatureViewModel
import com.slothiesmooth.nyx.shared.data.id.StegoImageId

/** Resolves [VaultDetailViewModel], loads [imageId], and pops on [VaultDetailUiEvent.Closed]. */
@Composable
fun VaultDetailScreen(
    imageId: String,
    onBack: () -> Unit,
    onDecryptThis: (String) -> Unit,
) {
    val viewModel = koinFeatureViewModel<VaultDetailViewModel>()
    LaunchedEffect(imageId) { viewModel.load(StegoImageId(imageId)) }
    LaunchedEffect(viewModel) {
        viewModel.state.uiEvent.collect { event -> if (event is VaultDetailUiEvent.Closed) onBack() }
    }
    VaultDetailContent(
        state = viewModel.state,
        onBack = onBack,
        onArchive = viewModel::archive,
        onRestore = viewModel::restore,
        onDelete = viewModel::delete,
        onShare = viewModel::share,
        onDecrypt = { onDecryptThis(imageId) },
    )
}
