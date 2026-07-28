package com.slothiesmooth.nyx.client

import com.slothiesmooth.nyx.shared.data.result.AppError
import com.slothiesmooth.nyx.shared.data.result.AppResult
import com.slothiesmooth.nyx.shared.data.source.ShareSource
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.dataWithBytes
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

/** iOS "share" presents a UIActivityViewController over a temp-file NSURL for the stego PNG. */
class IosShareSource : ShareSource {

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun shareImage(bytes: ByteArray, fileName: String): AppResult<Unit> = runCatching {
        val filePath = NSTemporaryDirectory() + fileName
        val nsData = bytes.usePinned { pinned ->
            NSData.dataWithBytes(pinned.addressOf(0), bytes.size.toULong())
        }
        nsData.writeToFile(filePath, atomically = true)
        val fileUrl = NSURL.fileURLWithPath(filePath)
        val controller = UIActivityViewController(
            activityItems = listOf(fileUrl),
            applicationActivities = null,
        )
        UIApplication.sharedApplication.keyWindow?.rootViewController
            ?.presentViewController(controller, animated = true, completion = null)
        AppResult.Ok(Unit)
    }.getOrElse { failure -> AppResult.Err(AppError.Storage(failure.message ?: "Share failed", failure)) }
}
