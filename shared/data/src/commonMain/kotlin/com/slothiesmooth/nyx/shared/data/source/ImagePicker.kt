package com.slothiesmooth.nyx.shared.data.source

/**
 * Picks an existing image from the platform's file chooser. Mirrors [CameraSource] as an injectable
 * seam: the real implementation shows the system chooser, while tests swap in a fake that returns a
 * preloaded image, so the encrypt/decrypt paths run without user interaction.
 */
interface ImagePicker {
    /** Shows the system image chooser and reads the selection; returns null when the user cancels. */
    suspend fun pickImage(): PickedImage?
}
