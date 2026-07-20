package com.slothiesmooth.nyx.shared.presentation.image

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Decodes encoded image bytes (PNG/JPEG) into a Compose [ImageBitmap] for display.
 * Decoding is platform work and never belongs in a composable — call this from a ViewModel
 * on a background dispatcher and expose the resulting [ImageBitmap] as render-ready state.
 */
expect fun ByteArray.toImageBitmap(): ImageBitmap
