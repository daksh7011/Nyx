package com.slothiesmooth.nyx.desktop

import com.slothiesmooth.nyx.shared.data.source.ImagePicker
import com.slothiesmooth.nyx.shared.data.source.PickedImage
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes

/** Desktop image chooser via FileKit's suspend picker (mirrors the android FileKitImagePicker). */
class DesktopImagePicker : ImagePicker {

    override suspend fun pickImage(): PickedImage? {
        val file = FileKit.openFilePicker(type = FileKitType.Image, mode = FileKitMode.Single) ?: return null
        return PickedImage(bytes = file.readBytes(), suggestedName = file.name)
    }
}
