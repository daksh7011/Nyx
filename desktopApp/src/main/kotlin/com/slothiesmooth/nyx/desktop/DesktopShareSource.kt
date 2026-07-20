package com.slothiesmooth.nyx.desktop

import com.slothiesmooth.nyx.shared.data.result.AppError
import com.slothiesmooth.nyx.shared.data.result.AppResult
import com.slothiesmooth.nyx.shared.data.source.ShareSource
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.write

/** Desktop "share" is a native Save-As dialog that writes the stego PNG to the chosen location. */
class DesktopShareSource : ShareSource {

    override suspend fun shareImage(bytes: ByteArray, fileName: String): AppResult<Unit> {
        val baseName = fileName.substringBeforeLast('.')
        val destination = FileKit.openFileSaver(suggestedName = baseName, defaultExtension = "png")
            ?: return AppResult.Ok(Unit) // user cancelled the dialog — not an error
        return runCatching { destination.write(bytes) }.fold(
            onSuccess = { AppResult.Ok(Unit) },
            onFailure = { failure -> AppResult.Err(AppError.Storage("Save failed", failure)) },
        )
    }
}
