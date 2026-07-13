package com.slothiesmooth.nyx.shared.data.id

import kotlin.jvm.JvmInline

/**
 * Type-safe identifier for a stored stego image. Wraps the raw uuid string.
 */
@JvmInline
value class StegoImageId(val value: String)
