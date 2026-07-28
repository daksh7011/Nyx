package com.slothiesmooth.nyx.feature.encrypt.basic.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slothiesmooth.nyx.designlibrary.atoms.NxButton
import com.slothiesmooth.nyx.designlibrary.atoms.NxButtonStyle
import com.slothiesmooth.nyx.designlibrary.atoms.NxField
import com.slothiesmooth.nyx.designlibrary.atoms.NxPasswordField
import com.slothiesmooth.nyx.designlibrary.atoms.NxText
import com.slothiesmooth.nyx.designlibrary.molecules.NxImageTile
import com.slothiesmooth.nyx.designlibrary.molecules.NxProgressOverlay
import com.slothiesmooth.nyx.designlibrary.templates.NxWizardTemplate
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.designlibrary.tokens.NxTextStyle
import com.slothiesmooth.nyx.designlibrary.tokens.nxColors
import com.slothiesmooth.nyx.feature.encrypt.basic.resources.Res
import com.slothiesmooth.nyx.feature.encrypt.basic.resources.encrypt_another
import com.slothiesmooth.nyx.feature.encrypt.basic.resources.encrypt_choose_image
import com.slothiesmooth.nyx.feature.encrypt.basic.resources.encrypt_confirm_password_label
import com.slothiesmooth.nyx.feature.encrypt.basic.resources.encrypt_continue
import com.slothiesmooth.nyx.feature.encrypt.basic.resources.encrypt_encrypted_image_content_description
import com.slothiesmooth.nyx.feature.encrypt.basic.resources.encrypt_message_label
import com.slothiesmooth.nyx.feature.encrypt.basic.resources.encrypt_message_prompt
import com.slothiesmooth.nyx.feature.encrypt.basic.resources.encrypt_password_label
import com.slothiesmooth.nyx.feature.encrypt.basic.resources.encrypt_password_prompt
import com.slothiesmooth.nyx.feature.encrypt.basic.resources.encrypt_pick_prompt
import com.slothiesmooth.nyx.feature.encrypt.basic.resources.encrypt_progress
import com.slothiesmooth.nyx.feature.encrypt.basic.resources.encrypt_result_heading
import com.slothiesmooth.nyx.feature.encrypt.basic.resources.encrypt_selected_image_content_description
import com.slothiesmooth.nyx.feature.encrypt.basic.resources.encrypt_share
import com.slothiesmooth.nyx.feature.encrypt.basic.resources.encrypt_step_done
import com.slothiesmooth.nyx.feature.encrypt.basic.resources.encrypt_step_image
import com.slothiesmooth.nyx.feature.encrypt.basic.resources.encrypt_step_message
import com.slothiesmooth.nyx.feature.encrypt.basic.resources.encrypt_step_password
import com.slothiesmooth.nyx.feature.encrypt.basic.resources.encrypt_submit
import com.slothiesmooth.nyx.feature.encrypt.basic.resources.encrypt_take_photo
import com.slothiesmooth.nyx.feature.encrypt.basic.resources.encrypt_title
import com.slothiesmooth.nyx.shared.presentation.state.UiState
import com.slothiesmooth.nyx.shared.presentation.util.asString
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource

/**
 * Stateless wizard body: renders the active step over [NxWizardTemplate] and overlays a blocking
 * spinner while the encrypt job runs. The four steps are image -> message -> password -> result, each
 * single-purpose with its own primary action. Every value is read from [state]; the composables never
 * validate, decode, or format. Previews live in EncryptContentPreviews.kt.
 */
@Composable
fun EncryptContent(
    state: EncryptState,
    onPickImage: () -> Unit = {},
    onCaptureImage: () -> Unit = {},
    onImageContinue: () -> Unit = {},
    onMessageChange: (String) -> Unit = {},
    onMessageContinue: () -> Unit = {},
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
            stringResource(Res.string.encrypt_step_password),
            stringResource(Res.string.encrypt_step_done),
        )
        NxWizardTemplate(
            stepLabels = stepLabels,
            currentStep = state.step.ordinal,
            title = stringResource(Res.string.encrypt_title),
            onBack = onBack,
        ) {
            when (state.step) {
                EncryptStep.PickImage -> PickImageStep(state, onPickImage, onCaptureImage, onImageContinue)
                EncryptStep.Message -> MessageStep(state, onMessageChange, onMessageContinue)
                EncryptStep.Password -> PasswordStep(state, onPasswordChange, onConfirmChange, onEncrypt)
                EncryptStep.Result -> ResultStep(state, onShare, onReset)
            }
        }
        if (state.uiState is UiState.Blocking) {
            NxProgressOverlay(label = stringResource(Res.string.encrypt_progress), modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun PickImageStep(
    state: EncryptState,
    onPickImage: () -> Unit,
    onCaptureImage: () -> Unit,
    onContinue: () -> Unit,
) {
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
        style = if (state.hasImage) NxButtonStyle.Secondary else NxButtonStyle.Primary,
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
    if (state.hasImage) {
        NxButton(
            text = stringResource(Res.string.encrypt_continue),
            onClick = onContinue,
            style = NxButtonStyle.Primary,
            block = true,
            leadingIcon = NxIconKind.ChevronRight,
        )
    }
}

@Composable
private fun MessageStep(
    state: EncryptState,
    onMessageChange: (String) -> Unit,
    onContinue: () -> Unit,
) {
    NxText(
        text = stringResource(Res.string.encrypt_message_prompt),
        style = NxTextStyle.Body,
        color = MaterialTheme.nxColors.fgMuted,
    )
    state.maxCharsLabel?.let { label ->
        NxText(text = label.asString(), style = NxTextStyle.Caption, color = MaterialTheme.nxColors.fgMuted)
    }
    NxField(
        value = state.message,
        onValueChange = onMessageChange,
        label = stringResource(Res.string.encrypt_message_label),
        multiline = true,
        modifier = Modifier.fillMaxWidth(),
    )
    NxButton(
        text = stringResource(Res.string.encrypt_continue),
        onClick = onContinue,
        style = NxButtonStyle.Primary,
        block = true,
        leadingIcon = NxIconKind.ChevronRight,
        enabled = state.messageReady,
    )
}

@Composable
private fun PasswordStep(
    state: EncryptState,
    onPasswordChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
    onEncrypt: () -> Unit,
) {
    NxText(
        text = stringResource(Res.string.encrypt_password_prompt),
        style = NxTextStyle.Body,
        color = MaterialTheme.nxColors.fgMuted,
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
