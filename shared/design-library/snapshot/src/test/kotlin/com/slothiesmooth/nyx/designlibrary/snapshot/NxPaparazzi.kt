package com.slothiesmooth.nyx.designlibrary.snapshot

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams

/**
 * Shared Paparazzi factory: renders on a Pixel 5 and SHRINKs the frame to each sample's own bounds.
 * Recorded on Linux (dev machine) to match the ubuntu CI runner's font rendering.
 */
fun nxPaparazzi(): Paparazzi = Paparazzi(
    deviceConfig = DeviceConfig.PIXEL_5,
    renderingMode = SessionParams.RenderingMode.SHRINK,
    showSystemUi = false,
)
