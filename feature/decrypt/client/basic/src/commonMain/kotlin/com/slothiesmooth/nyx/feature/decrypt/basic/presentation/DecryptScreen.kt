package com.slothiesmooth.nyx.feature.decrypt.basic.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.slothiesmooth.nyx.feature.common.koin.koinFeatureViewModel

/**
 * Thin host for the reveal flow: resolves [DecryptViewModel] from the isolated graph, loads any
 * vault image on entry, and delegates image picking and clipboard copy to the view model. All logic
 * stays in the view model.
 */
@Composable
fun DecryptScreen(
    imageId: String?,
    onBack: () -> Unit,
) {
    val viewModel = koinFeatureViewModel<DecryptViewModel>()
    LaunchedEffect(imageId) { viewModel.load(imageId) }
    DecryptContent(
        state = viewModel.state,
        onPickImage = viewModel::onPickImage,
        onPasswordChange = viewModel::onPasswordChange,
        onDecrypt = viewModel::decrypt,
        onCopy = viewModel::onCopy,
        onBack = onBack,
    )
}
