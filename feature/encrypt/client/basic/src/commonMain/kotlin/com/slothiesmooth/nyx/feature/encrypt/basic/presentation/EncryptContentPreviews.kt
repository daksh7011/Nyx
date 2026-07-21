package com.slothiesmooth.nyx.feature.encrypt.basic.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.slothiesmooth.nyx.designlibrary.tokens.AllThemePreview
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.designlibrary.tokens.NxPaletteProvider
import com.slothiesmooth.nyx.designlibrary.tokens.NxTheme
import com.slothiesmooth.nyx.shared.presentation.state.UiState
import com.slothiesmooth.nyx.shared.presentation.text.UiText

private const val PREVIEW_THUMBNAIL_PX = 320

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
    this.maxCharsLabel = UiText.raw("About 1.8k characters fit")
    if (blocking) uiState = UiState.Blocking
}

private fun previewThumbnail(): ImageBitmap = ImageBitmap(width = PREVIEW_THUMBNAIL_PX, height = PREVIEW_THUMBNAIL_PX)

@AllThemePreview
@Composable
private fun EncryptPickPreview(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { EncryptContent(previewState(EncryptStep.PickImage)) }
}

@AllThemePreview
@Composable
private fun EncryptPickWithImagePreview(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { EncryptContent(previewState(EncryptStep.PickImage, thumbnail = previewThumbnail())) }
}

@AllThemePreview
@Composable
private fun EncryptMessageEmptyPreview(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) { EncryptContent(previewState(EncryptStep.Message, thumbnail = previewThumbnail())) }
}

@AllThemePreview
@Composable
private fun EncryptMessageReadyPreview(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    val state = previewState(EncryptStep.Message, message = "meet me at dawn", thumbnail = previewThumbnail())
    NxTheme(palette) { EncryptContent(state) }
}

@AllThemePreview
@Composable
private fun EncryptPasswordErrorPreview(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    NxTheme(palette) {
        EncryptContent(
            previewState(
                EncryptStep.Password,
                message = "meet me",
                password = "pw",
                confirmPassword = "px",
                validationError = UiText.raw("Passwords do not match"),
                thumbnail = previewThumbnail(),
            ),
        )
    }
}

@AllThemePreview
@Composable
private fun EncryptPasswordReadyPreview(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    val state = previewState(
        EncryptStep.Password,
        message = "meet me",
        password = "pw",
        confirmPassword = "pw",
        canEncrypt = true,
        thumbnail = previewThumbnail(),
    )
    NxTheme(palette) { EncryptContent(state) }
}

@AllThemePreview
@Composable
private fun EncryptSaveFailedPreview(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    val state = previewState(
        EncryptStep.Password,
        message = "meet me",
        password = "pw",
        confirmPassword = "pw",
        canEncrypt = true,
        validationError = UiText.raw("Encrypted, but could not save to your vault. Try again."),
        thumbnail = previewThumbnail(),
    )
    NxTheme(palette) { EncryptContent(state) }
}

@AllThemePreview
@Composable
private fun EncryptEncryptingPreview(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    val state = previewState(
        EncryptStep.Password,
        message = "meet me",
        password = "pw",
        confirmPassword = "pw",
        canEncrypt = true,
        blocking = true,
        thumbnail = previewThumbnail(),
    )
    NxTheme(palette) { EncryptContent(state) }
}

@AllThemePreview
@Composable
private fun EncryptResultPreview(@PreviewParameter(NxPaletteProvider::class) palette: NxPalette) {
    val state = previewState(EncryptStep.Result, savedName = "nyx-abcdef12.png", thumbnail = previewThumbnail())
    NxTheme(palette) { EncryptContent(state) }
}
