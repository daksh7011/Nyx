package com.slothiesmooth.nyx.feature.decrypt.basic.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.slothiesmooth.nyx.feature.common.koin.koinFeatureViewModel
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.launch

/**
 * Thin host for the reveal flow: resolves [DecryptViewModel] from the isolated graph, loads any
 * vault image on entry, owns the FileKit image picker for the pick-from-device path, and copies a
 * revealed message to the clipboard. All logic stays in the view model.
 */
@Composable
fun DecryptScreen(
    imageId: String?,
    onBack: () -> Unit,
) {
    val viewModel = koinFeatureViewModel<DecryptViewModel>()
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    LaunchedEffect(imageId) { viewModel.load(imageId) }
    val picker = rememberFilePickerLauncher(type = FileKitType.Image, mode = FileKitMode.Single) { file ->
        if (file != null) scope.launch { viewModel.onImagePicked(file.readBytes()) }
    }
    DecryptContent(
        state = viewModel.state,
        onPickImage = { picker.launch() },
        onPasswordChange = viewModel::onPasswordChange,
        onDecrypt = viewModel::decrypt,
        onCopy = { text -> clipboard.setText(AnnotatedString(text)) },
        onBack = onBack,
    )
}
