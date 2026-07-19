package com.slothiesmooth.nyx.feature.encrypt.basic.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.slothiesmooth.nyx.designlibrary.atoms.NxButton
import com.slothiesmooth.nyx.designlibrary.atoms.NxButtonStyle
import com.slothiesmooth.nyx.designlibrary.atoms.NxField
import com.slothiesmooth.nyx.designlibrary.atoms.NxPasswordField
import com.slothiesmooth.nyx.designlibrary.atoms.NxText
import com.slothiesmooth.nyx.designlibrary.molecules.NxImageTile
import com.slothiesmooth.nyx.designlibrary.molecules.NxProgressOverlay
import com.slothiesmooth.nyx.designlibrary.templates.NxWizardTemplate
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTextStyle
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.designlibrary.tokens.nxColors
import com.slothiesmooth.nyx.feature.encrypt.basic.resources.Res
import com.slothiesmooth.nyx.feature.encrypt.basic.resources.encrypt_another
import com.slothiesmooth.nyx.feature.encrypt.basic.resources.encrypt_choose_image
import com.slothiesmooth.nyx.feature.encrypt.basic.resources.encrypt_confirm_password_label
import com.slothiesmooth.nyx.feature.encrypt.basic.resources.encrypt_encrypted_image_content_description
import com.slothiesmooth.nyx.feature.encrypt.basic.resources.encrypt_message_label
import com.slothiesmooth.nyx.feature.encrypt.basic.resources.encrypt_password_label
import com.slothiesmooth.nyx.feature.encrypt.basic.resources.encrypt_pick_prompt
import com.slothiesmooth.nyx.feature.encrypt.basic.resources.encrypt_progress
import com.slothiesmooth.nyx.feature.encrypt.basic.resources.encrypt_result_heading
import com.slothiesmooth.nyx.feature.encrypt.basic.resources.encrypt_selected_image_content_description
import com.slothiesmooth.nyx.feature.encrypt.basic.resources.encrypt_share
import com.slothiesmooth.nyx.feature.encrypt.basic.resources.encrypt_step_done
import com.slothiesmooth.nyx.feature.encrypt.basic.resources.encrypt_step_image
import com.slothiesmooth.nyx.feature.encrypt.basic.resources.encrypt_step_message
import com.slothiesmooth.nyx.feature.encrypt.basic.resources.encrypt_submit
import com.slothiesmooth.nyx.feature.encrypt.basic.resources.encrypt_take_photo
import com.slothiesmooth.nyx.feature.encrypt.basic.resources.encrypt_title
import com.slothiesmooth.nyx.shared.presentation.state.UiState
import com.slothiesmooth.nyx.shared.presentation.text.UiText
import com.slothiesmooth.nyx.shared.presentation.util.asString
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource

private const val PREVIEW_THUMBNAIL_PX = 320

/**
 * Stateless wizard body: renders the active step over [NxWizardTemplate] and overlays a blocking
 * spinner while the encrypt job runs. Every value is read from [state]; the composables never
 * validate, decode, or format.
 */
@Composable
fun EncryptContent(
    state: EncryptState,
    onPickImage: () -> Unit = {},
    onCaptureImage: () -> Unit = {},
    onMessageChange: (String) -> Unit = {},
    onPasswordChange: (String) -> Unit = {},
    onConfirmChange: (String) -> Unit = {},
    onEncrypt: () -> Unit = {},
    onShare: () -> Unit = {},
    onReset: () -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        val stepLabels = persistentListOf(
            stringResource(Res.string.encrypt_step_image),
            stringResource(Res.string.encrypt_step_message),
            stringResource(Res.string.encrypt_step_done),
        )
        NxWizardTemplate(
            stepLabels = stepLabels,
            currentStep = state.step.ordinal,
            title = stringResource(Res.string.encrypt_title),
            onBack = onBack,
        ) {
            when (state.step) {
                EncryptStep.PickImage -> PickImageStep(state, onPickImage, onCaptureImage)
                EncryptStep.Compose -> ComposeStep(state, onMessageChange, onPasswordChange, onConfirmChange, onEncrypt)
                EncryptStep.Result -> ResultStep(state, onShare, onReset)
            }
        }
        if (state.uiState is UiState.Blocking) {
            NxProgressOverlay(label = stringResource(Res.string.encrypt_progress), modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun PickImageStep(state: EncryptState, onPickImage: () -> Unit, onCaptureImage: () -> Unit) {
    if (state.hasImage) {
        NxImageTile(
            image = state.thumbnail,
            contentDescription = stringResource(Res.string.encrypt_selected_image_content_description),
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        )
    }
    NxText(
        text = stringResource(Res.string.encrypt_pick_prompt),
        style = NxTextStyle.Body,
        color = MaterialTheme.nxColors.fgMuted,
    )
    NxButton(
        text = stringResource(Res.string.encrypt_choose_image),
        onClick = onPickImage,
        style = NxButtonStyle.Primary,
        block = true,
        leadingIcon = NxIconKind.Image,
    )
    if (state.showCamera) {
        NxButton(
            text = stringResource(Res.string.encrypt_take_photo),
            onClick = onCaptureImage,
            style = NxButtonStyle.Secondary,
            block = true,
            leadingIcon = NxIconKind.Camera,
        )
    }
}

@Composable
private fun ComposeStep(
    state: EncryptState,
    onMessageChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
    onEncrypt: () -> Unit,
) {
    NxImageTile(
        image = state.thumbnail,
        contentDescription = stringResource(Res.string.encrypt_selected_image_content_description),
        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
    )
    state.maxCharsLabel?.let { label ->
        NxText(text = label.asString(), style = NxTextStyle.Caption, color = MaterialTheme.nxColors.fgMuted)
    }
    NxField(
        value = state.message,
        onValueChange = onMessageChange,
        label = stringResource(Res.string.encrypt_message_label),
        multiline = true,
    )
    NxPasswordField(
        value = state.password,
        onValueChange = onPasswordChange,
        label = stringResource(Res.string.encrypt_password_label),
    )
    NxPasswordField(
        value = state.confirmPassword,
        onValueChange = onConfirmChange,
        label = stringResource(Res.string.encrypt_confirm_password_label),
    )
    val error = state.validationError
    if (error != null) {
        NxText(text = error.asString(), style = NxTextStyle.Caption, color = MaterialTheme.nxColors.danger)
    }
    NxButton(
        text = stringResource(Res.string.encrypt_submit),
        onClick = onEncrypt,
        style = NxButtonStyle.Primary,
        block = true,
        leadingIcon = NxIconKind.Lock,
        enabled = state.canEncrypt,
    )
}

@Composable
private fun ResultStep(state: EncryptState, onShare: () -> Unit, onReset: () -> Unit) {
    NxImageTile(
        image = state.thumbnail,
        contentDescription = stringResource(Res.string.encrypt_encrypted_image_content_description),
        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
    )
    NxText(text = stringResource(Res.string.encrypt_result_heading), style = NxTextStyle.Heading)
    NxText(text = state.savedName ?: "", style = NxTextStyle.Caption, color = MaterialTheme.nxColors.fgMuted)
    NxButton(
        text = stringResource(Res.string.encrypt_share),
        onClick = onShare,
        style = NxButtonStyle.Primary,
        block = true,
        leadingIcon = NxIconKind.Share,
    )
    NxButton(
        text = stringResource(Res.string.encrypt_another),
        onClick = onReset,
        style = NxButtonStyle.Ghost,
        block = true,
        leadingIcon = NxIconKind.Plus,
    )
}

private fun previewState(
    step: EncryptStep,
    message: String = "",
    password: String = "",
    confirmPassword: String = "",
    validationError: UiText? = null,
    canEncrypt: Boolean = false,
    savedName: String? = null,
    thumbnail: ImageBitmap? = null,
    blocking: Boolean = false,
): EncryptMutableState = EncryptMutableState(cameraVisible = true).apply {
    this.step = step
    this.message = message
    this.password = password
    this.confirmPassword = confirmPassword
    this.validationError = validationError
    this.canEncrypt = canEncrypt
    this.savedName = savedName
    this.thumbnail = thumbnail
    this.maxCharsLabel = UiText.raw("About 1820 characters fit")
    if (blocking) uiState = UiState.Blocking
}

@AllThemePreview
@Composable
private fun EncryptPickPreview(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { EncryptContent(previewState(EncryptStep.PickImage)) }
}

@AllThemePreview
@Composable
private fun EncryptPickWithImagePreview(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    val state = previewState(
        EncryptStep.PickImage,
        thumbnail = ImageBitmap(width = PREVIEW_THUMBNAIL_PX, height = PREVIEW_THUMBNAIL_PX),
    )
    NxTheme(palette) { EncryptContent(state) }
}

@AllThemePreview
@Composable
private fun EncryptComposeErrorPreview(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) {
        EncryptContent(
            previewState(
                EncryptStep.Compose,
                message = "meet me",
                password = "pw",
                confirmPassword = "px",
                validationError = UiText.raw("Passwords do not match"),
            ),
        )
    }
}

@AllThemePreview
@Composable
private fun EncryptComposeReadyPreview(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    val state = previewState(
        EncryptStep.Compose,
        message = "meet me",
        password = "pw",
        confirmPassword = "pw",
        canEncrypt = true,
    )
    NxTheme(palette) { EncryptContent(state) }
}

@AllThemePreview
@Composable
private fun EncryptEncryptingPreview(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    val state = previewState(
        EncryptStep.Compose,
        message = "meet me",
        password = "pw",
        confirmPassword = "pw",
        blocking = true,
    )
    NxTheme(palette) { EncryptContent(state) }
}

@AllThemePreview
@Composable
private fun EncryptResultPreview(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { EncryptContent(previewState(EncryptStep.Result, savedName = "nyx-abcdef12.png")) }
}
