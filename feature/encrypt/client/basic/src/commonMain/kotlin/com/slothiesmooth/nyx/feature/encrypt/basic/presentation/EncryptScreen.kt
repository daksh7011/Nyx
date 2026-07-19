package com.slothiesmooth.nyx.feature.encrypt.basic.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.slothiesmooth.nyx.feature.common.koin.koinFeatureViewModel
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.launch

/**
 * Thin host for the encrypt wizard: resolves [EncryptViewModel] from the isolated graph, owns the
 * FileKit image picker, and forwards user actions. The back action closes the flow only on the first
 * step; otherwise it walks the wizard back a step.
 */
@Composable
fun EncryptScreen(onClose: () -> Unit) {
    val viewModel = koinFeatureViewModel<EncryptViewModel>()
    val scope = rememberCoroutineScope()
    val picker = rememberFilePickerLauncher(type = FileKitType.Image, mode = FileKitMode.Single) { file ->
        if (file != null) scope.launch { viewModel.onImagePicked(file.readBytes()) }
    }
    EncryptContent(
        state = viewModel.state,
        onPickImage = { picker.launch() },
        onCaptureImage = viewModel::onCameraCapture,
        onMessageChange = viewModel::onMessageChange,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmChange = viewModel::onConfirmChange,
        onEncrypt = viewModel::encrypt,
        onShare = viewModel::share,
        onReset = viewModel::reset,
        onBack = { if (viewModel.state.step == EncryptStep.PickImage) onClose() else viewModel.back() },
    )
}
