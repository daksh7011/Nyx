package com.slothiesmooth.nyx.client

import com.slothiesmooth.nyx.shared.data.result.AppError
import com.slothiesmooth.nyx.shared.data.result.AppResult
import com.slothiesmooth.nyx.shared.data.source.VaultFileStore
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.list
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.write

/**
 * Stores stego PNG bytes as `<root>/<id>.png`. [root] is the iOS vault directory (mirrors android).
 * The directory is (re)created before every write because kotlinx-io's sink does not create missing
 * parents, so a fresh install would otherwise fail the first save.
 */
class FileKitVaultFileStore(private val root: PlatformFile) : VaultFileStore {

    private fun fileFor(id: String): PlatformFile = PlatformFile(root, "$id.png")

    override suspend fun write(id: String, bytes: ByteArray): AppResult<Unit> = runCatching {
        root.createDirectories()
        fileFor(id).write(bytes)
        AppResult.Ok(Unit)
    }.getOrElse { failure -> AppResult.Err(AppError.Storage("Vault write failed", failure)) }

    override suspend fun read(id: String): AppResult<ByteArray> = runCatching {
        AppResult.Ok(fileFor(id).readBytes())
    }.getOrElse { failure -> AppResult.Err(AppError.Storage("Vault read failed", failure)) }

    override suspend fun delete(id: String): AppResult<Unit> = runCatching {
        fileFor(id).delete()
        AppResult.Ok(Unit)
    }.getOrElse { failure -> AppResult.Err(AppError.Storage("Vault delete failed", failure)) }

    override suspend fun deleteAll(): AppResult<Unit> = runCatching {
        if (root.exists()) root.list().forEach { file -> file.delete() }
        AppResult.Ok(Unit)
    }.getOrElse { failure -> AppResult.Err(AppError.Storage("Vault wipe failed", failure)) }
}
