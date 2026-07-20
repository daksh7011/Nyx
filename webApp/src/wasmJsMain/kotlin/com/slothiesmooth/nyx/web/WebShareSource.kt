package com.slothiesmooth.nyx.web

import com.slothiesmooth.nyx.shared.data.result.AppResult
import com.slothiesmooth.nyx.shared.data.source.ShareSource
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.download

/** Web "share" is a browser download of the stego PNG (FileKit.download is web-only). */
class WebShareSource : ShareSource {
    override suspend fun shareImage(bytes: ByteArray, fileName: String): AppResult<Unit> {
        FileKit.download(bytes = bytes, fileName = fileName)
        return AppResult.Ok(Unit)
    }
}
