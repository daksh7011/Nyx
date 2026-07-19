package com.slothiesmooth.nyx.feature.encrypt.basic.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import com.slothiesmooth.nyx.shared.presentation.state.UiState
import kotlinx.collections.immutable.persistentListOf

private val STEP_LABELS = persistentListOf("Image", "Message", "Done")

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
        NxWizardTemplate(
            stepLabels = STEP_LABELS,
            currentStep = state.step.ordinal,
            title = "Encrypt",
            onBack = onBack,
        ) {
            when (state.step) {
                EncryptStep.PickImage -> PickImageStep(state, onPickImage, onCaptureImage)
                EncryptStep.Compose -> ComposeStep(state, onMessageChange, onPasswordChange, onConfirmChange, onEncrypt)
                EncryptStep.Result -> ResultStep(state, onShare, onReset)
            }
        }
        if (state.uiState is UiState.Blocking) {
            NxProgressOverlay(label = "Encrypting", modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun PickImageStep(state: EncryptState, onPickImage: () -> Unit, onCaptureImage: () -> Unit) {
    NxText(
        text = "Choose a cover image to hide your message in.",
        style = NxTextStyle.Body,
        color = MaterialTheme.nxColors.fgMuted,
    )
    NxButton(
        text = "Choose image",
        onClick = onPickImage,
        style = NxButtonStyle.Primary,
        block = true,
        leadingIcon = NxIconKind.Image,
    )
    if (state.showCamera) {
        NxButton(
            text = "Take photo",
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
        contentDescription = "Selected image",
        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
    )
    NxText(text = state.maxCharsLabel, style = NxTextStyle.Caption, color = MaterialTheme.nxColors.fgMuted)
    NxField(value = state.message, onValueChange = onMessageChange, label = "Secret message", multiline = true)
    NxPasswordField(value = state.password, onValueChange = onPasswordChange, label = "Password")
    NxPasswordField(value = state.confirmPassword, onValueChange = onConfirmChange, label = "Confirm password")
    val error = state.validationError
    if (error != null) {
        NxText(text = error, style = NxTextStyle.Caption, color = MaterialTheme.nxColors.danger)
    }
    NxButton(
        text = "Encrypt",
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
        contentDescription = "Encrypted image",
        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
    )
    NxText(text = "Encrypted and saved", style = NxTextStyle.Heading)
    NxText(text = state.savedName ?: "", style = NxTextStyle.Caption, color = MaterialTheme.nxColors.fgMuted)
    NxButton(
        text = "Share image",
        onClick = onShare,
        style = NxButtonStyle.Primary,
        block = true,
        leadingIcon = NxIconKind.Share,
    )
    NxButton(
        text = "Encrypt another",
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
    validationError: String? = null,
    canEncrypt: Boolean = false,
    savedName: String? = null,
    blocking: Boolean = false,
): EncryptMutableState = EncryptMutableState(cameraVisible = true).apply {
    this.step = step
    this.message = message
    this.password = password
    this.confirmPassword = confirmPassword
    this.validationError = validationError
    this.canEncrypt = canEncrypt
    this.savedName = savedName
    this.maxCharsLabel = "About 1820 characters fit"
    if (blocking) uiState = UiState.Blocking
}

@AllThemePreview
@Composable
private fun EncryptPickPreview(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { EncryptContent(previewState(EncryptStep.PickImage)) }
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
                validationError = "Passwords do not match",
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
