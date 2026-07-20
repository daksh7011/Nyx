package com.slothiesmooth.nyx.shared.data.source

import com.slothiesmooth.nyx.shared.data.result.AppResult

/**
 * Exports a PNG: Android/iOS share sheet, desktop save dialog, web browser download.
 */
interface ShareSource {
    suspend fun shareImage(bytes: ByteArray, fileName: String): AppResult<Unit>
}
