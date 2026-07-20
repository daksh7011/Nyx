package com.slothiesmooth.nyx.client

import com.slothiesmooth.nyx.shared.data.source.CameraSource
import com.slothiesmooth.nyx.shared.data.source.PickedImage
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openCameraPicker
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes

/**
 * System-camera capture via FileKit (mobile only). Requires NSCameraUsageDescription in Info.plist.
 * Mirrors the android FileKitCameraSource; only the package differs.
 */
class FileKitCameraSource : CameraSource {

    override val isAvailable: Boolean = true

    override suspend fun capture(): PickedImage? {
        val file = FileKit.openCameraPicker() ?: return null
        return PickedImage(bytes = file.readBytes(), suggestedName = file.name)
    }
}
