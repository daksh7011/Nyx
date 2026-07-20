package com.slothiesmooth.nyx.desktop

import com.slothiesmooth.nyx.shared.data.source.CameraSource
import com.slothiesmooth.nyx.shared.data.source.PickedImage

/** Desktop has no system camera; `PlatformCapabilities(camera = false)` hides the capture button. */
object NoCameraSource : CameraSource {
    override val isAvailable: Boolean = false
    override suspend fun capture(): PickedImage? = null
}
