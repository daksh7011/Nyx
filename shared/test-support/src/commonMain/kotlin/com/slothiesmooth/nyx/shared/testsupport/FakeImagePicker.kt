package com.slothiesmooth.nyx.shared.testsupport

import com.slothiesmooth.nyx.shared.data.source.ImagePicker
import com.slothiesmooth.nyx.shared.data.source.PickedImage

/**
 * In-memory [ImagePicker] for tests: each pick returns [next] (a preloaded base cover or a stego
 * result), letting a test drive the encrypt/decrypt paths without the system chooser. Set [next] to
 * null to simulate a cancelled chooser; [pickCount] records how many picks occurred.
 */
class FakeImagePicker(var next: PickedImage? = null) : ImagePicker {

    var pickCount: Int = 0
        private set

    override suspend fun pickImage(): PickedImage? {
        pickCount++
        return next
    }
}
