package com.slothiesmooth.nyx.shared.data.source

/**
 * Feature flags a platform module reports so the UI can hide unsupported affordances (e.g. no
 * camera or no persistent vault on web).
 */
data class PlatformCapabilities(val camera: Boolean, val persistentVault: Boolean)
