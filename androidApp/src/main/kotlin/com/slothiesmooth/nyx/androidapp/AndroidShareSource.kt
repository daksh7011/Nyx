package com.slothiesmooth.nyx.androidapp

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.slothiesmooth.nyx.shared.data.result.AppError
import com.slothiesmooth.nyx.shared.data.result.AppResult
import com.slothiesmooth.nyx.shared.data.source.ShareSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val SHARE_SUBDIR = "shared"
private const val MIME_PNG = "image/png"

/** Shares stego PNG bytes through a system chooser via a FileProvider content URI. */
class AndroidShareSource(private val context: Context) : ShareSource {

    override suspend fun shareImage(bytes: ByteArray, fileName: String): AppResult<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(context.cacheDir, SHARE_SUBDIR).apply { mkdirs() }
                val file = File(dir, fileName).apply { writeBytes(bytes) }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = MIME_PNG
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(
                    Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
                AppResult.Ok(Unit)
            }.getOrElse { failure -> AppResult.Err(AppError.Storage("Share failed", failure)) }
        }
}
