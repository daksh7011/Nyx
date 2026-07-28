package com.slothiesmooth.nyx.feature.encrypt.basic.presentation

import androidx.compose.runtime.Composable
import com.slothiesmooth.nyx.feature.common.koin.koinFeatureViewModel

/**
 * Thin host for the encrypt wizard: resolves [EncryptViewModel] from the isolated graph and forwards
 * user actions. Image picking runs through the injected picker inside the view model. The back action
 * closes the flow only on the first step; otherwise it walks the wizard back a step.
 */
@Composable
fun EncryptScreen(onClose: () -> Unit) {
    val viewModel = koinFeatureViewModel<EncryptViewModel>()
    EncryptContent(
        state = viewModel.state,
        onPickImage = viewModel::onPickImage,
        onCaptureImage = viewModel::onCameraCapture,
        onImageContinue = viewModel::onImageContinue,
        onMessageChange = viewModel::onMessageChange,
        onMessageContinue = viewModel::onMessageContinue,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmChange = viewModel::onConfirmChange,
        onEncrypt = viewModel::encrypt,
        onShare = viewModel::share,
        onReset = viewModel::reset,
        onBack = { if (viewModel.state.step == EncryptStep.PickImage) onClose() else viewModel.back() },
    )
}
