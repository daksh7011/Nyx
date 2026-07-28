package com.slothiesmooth.nyx.shared.data.source

/**
 * Writes plain text to the system clipboard. Implemented per platform entry module (Android in this
 * phase; desktop/web/iOS in plan 07) and injected via the platform Koin module — the same seam as
 * [ShareSource]/[CameraSource], which keeps clipboard access out of composables and testable.
 */
interface ClipboardWriter {
    suspend fun copyPlainText(text: String)
}
