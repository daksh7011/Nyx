package com.slothiesmooth.nyx.client.data.codec

import com.slothiesmooth.nyx.shared.data.source.ImageCodec

/** Platform image codec: Android `BitmapFactory`, everything else skiko. The 00-INDEX expect/actual. */
expect fun defaultImageCodec(): ImageCodec
