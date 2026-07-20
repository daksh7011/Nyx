package com.slothiesmooth.nyx.shared.testsupport

import com.slothiesmooth.nyx.shared.data.result.AppResult
import com.slothiesmooth.nyx.shared.data.source.ShareSource

/** In-memory [ShareSource]; records the last shared payload and returns a configurable [result]. */
class FakeShareSource(var result: AppResult<Unit> = AppResult.Ok(Unit)) : ShareSource {
    var lastShared: Pair<ByteArray, String>? = null

    override suspend fun shareImage(bytes: ByteArray, fileName: String): AppResult<Unit> {
        lastShared = bytes to fileName
        return result
    }
}
