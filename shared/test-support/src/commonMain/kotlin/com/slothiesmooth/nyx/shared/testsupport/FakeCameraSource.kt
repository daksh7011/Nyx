package com.slothiesmooth.nyx.shared.testsupport

import com.slothiesmooth.nyx.shared.data.source.CameraSource
import com.slothiesmooth.nyx.shared.data.source.PickedImage

/** In-memory [CameraSource]; [capture] returns [next]. */
class FakeCameraSource(override val isAvailable: Boolean = true) : CameraSource {
    var next: PickedImage? = null

    override suspend fun capture(): PickedImage? = next
}
