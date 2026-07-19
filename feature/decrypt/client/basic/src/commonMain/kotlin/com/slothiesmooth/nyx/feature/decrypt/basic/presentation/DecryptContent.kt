package com.slothiesmooth.nyx.feature.decrypt.basic.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.slothiesmooth.nyx.designlibrary.atoms.NxButton
import com.slothiesmooth.nyx.designlibrary.atoms.NxButtonStyle
import com.slothiesmooth.nyx.designlibrary.atoms.NxIconButton
import com.slothiesmooth.nyx.designlibrary.atoms.NxPasswordField
import com.slothiesmooth.nyx.designlibrary.atoms.NxText
import com.slothiesmooth.nyx.designlibrary.molecules.NxCard
import com.slothiesmooth.nyx.designlibrary.molecules.NxCardVariant
import com.slothiesmooth.nyx.designlibrary.molecules.NxImageTile
import com.slothiesmooth.nyx.designlibrary.molecules.NxProgressOverlay
import com.slothiesmooth.nyx.designlibrary.templates.NxDetailTemplate
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTextStyle
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.designlibrary.tokens.nxColors
import com.slothiesmooth.nyx.shared.presentation.state.UiEvent
import com.slothiesmooth.nyx.shared.presentation.state.UiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Stateless reveal body: renders the pick-or-image step, the password field, and — after a decrypt —
 * either the revealed message (with a copy affordance) or an honest error. Overlays a blocking
 * spinner while the decrypt job runs. Every value is read from [state]; the composables never decode,
 * validate, or map.
 */
@Composable
fun DecryptContent(
    state: DecryptState,
    onPickImage: () -> Unit = {},
    onPasswordChange: (String) -> Unit = {},
    onDecrypt: () -> Unit = {},
    onCopy: () -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        NxDetailTemplate(title = "Decrypt", onBack = onBack) {
            if (!state.isFromVault && !state.hasImage) {
                NxText(
                    text = "Choose an image that hides an encrypted message.",
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
            } else {
                NxImageTile(
                    image = state.thumbnail,
                    contentDescription = "Image to decrypt",
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                )
            }
            NxPasswordField(value = state.password, onValueChange = onPasswordChange, label = "Password")
            NxButton(
                text = "Reveal message",
                onClick = onDecrypt,
                style = NxButtonStyle.Primary,
                block = true,
                leadingIcon = NxIconKind.Unlock,
                enabled = state.canDecrypt,
            )
            val plaintext = state.plaintext
            if (plaintext != null) {
                RevealedMessage(plaintext = plaintext, onCopy = onCopy)
            }
            val error = state.errorMessage
            if (error != null) {
                NxText(text = error, style = NxTextStyle.Body, color = MaterialTheme.nxColors.danger)
            }
        }
        if (state.uiState is UiState.Blocking) {
            NxProgressOverlay(label = "Decrypting", modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun RevealedMessage(plaintext: String, onCopy: () -> Unit) {
    NxCard(variant = NxCardVariant.Flat) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            NxText(
                text = plaintext,
                style = NxTextStyle.Mono,
                color = MaterialTheme.nxColors.codeFg,
                modifier = Modifier.weight(1f),
            )
            NxIconButton(
                kind = NxIconKind.Copy,
                onClick = onCopy,
                contentDescription = "Copy message",
            )
        }
    }
}

private class PreviewDecryptState(
    override val hasImage: Boolean = false,
    override val isFromVault: Boolean = false,
    override val password: String = "",
    override val canDecrypt: Boolean = false,
    override val plaintext: String? = null,
    override val errorMessage: String? = null,
    override val uiState: UiState = UiState.Ready,
) : DecryptState {
    override val thumbnail: ImageBitmap? = null
    override val uiEvent: Flow<UiEvent> = emptyFlow()
}

@AllThemePreview
@Composable
private fun DecryptPickPreview(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { DecryptContent(state = PreviewDecryptState()) }
}

@AllThemePreview
@Composable
private fun DecryptRevealedPreview(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    val state = PreviewDecryptState(
        hasImage = true,
        isFromVault = true,
        password = "hunter2",
        canDecrypt = true,
        plaintext = "meet me at the north pier at 0500",
    )
    NxTheme(palette) { DecryptContent(state = state) }
}

@AllThemePreview
@Composable
private fun DecryptErrorPreview(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    val state = PreviewDecryptState(
        hasImage = true,
        isFromVault = true,
        password = "wrong",
        canDecrypt = true,
        errorMessage = "Wrong password, or this image has been tampered with.",
    )
    NxTheme(palette) { DecryptContent(state = state) }
}

@AllThemePreview
@Composable
private fun DecryptDecryptingPreview(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    val state = PreviewDecryptState(
        hasImage = true,
        isFromVault = true,
        password = "hunter2",
        canDecrypt = true,
        uiState = UiState.Blocking,
    )
    NxTheme(palette) { DecryptContent(state = state) }
}
