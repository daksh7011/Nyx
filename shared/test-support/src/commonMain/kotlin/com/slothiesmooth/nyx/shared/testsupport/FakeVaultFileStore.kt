package com.slothiesmooth.nyx.shared.testsupport

import com.slothiesmooth.nyx.shared.data.result.AppError
import com.slothiesmooth.nyx.shared.data.result.AppResult
import com.slothiesmooth.nyx.shared.data.source.VaultFileStore

/** In-memory [VaultFileStore]; [failNextWrite] forces one Storage error to test rollback paths. */
class FakeVaultFileStore : VaultFileStore {
    private val files = mutableMapOf<String, ByteArray>()
    var failNextWrite: Boolean = false

    override suspend fun write(id: String, bytes: ByteArray): AppResult<Unit> {
        if (failNextWrite) {
            failNextWrite = false
            return AppResult.Err(AppError.Storage("forced write failure"))
        }
        files[id] = bytes
        return AppResult.Ok(Unit)
    }

    override suspend fun read(id: String): AppResult<ByteArray> {
        val bytes = files[id] ?: return AppResult.Err(AppError.NotFound)
        return AppResult.Ok(bytes)
    }

    override suspend fun delete(id: String): AppResult<Unit> {
        files.remove(id)
        return AppResult.Ok(Unit)
    }

    override suspend fun deleteAll(): AppResult<Unit> {
        files.clear()
        return AppResult.Ok(Unit)
    }
}
