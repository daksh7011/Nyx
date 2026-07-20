package com.slothiesmooth.nyx.web

import com.slothiesmooth.nyx.shared.data.source.CameraSource
import com.slothiesmooth.nyx.shared.data.source.PickedImage

/** No camera on the web build; `PlatformCapabilities(camera = false)` hides the capture button. */
object NoCameraSource : CameraSource {
    override val isAvailable: Boolean = false
    override suspend fun capture(): PickedImage? = null
}
