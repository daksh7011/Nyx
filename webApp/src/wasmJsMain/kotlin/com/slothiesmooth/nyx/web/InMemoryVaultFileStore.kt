package com.slothiesmooth.nyx.web

import com.slothiesmooth.nyx.shared.data.result.AppError
import com.slothiesmooth.nyx.shared.data.result.AppResult
import com.slothiesmooth.nyx.shared.data.source.VaultFileStore

/** Session-only PNG store for wasm (single-threaded, so a plain map is safe). */
class InMemoryVaultFileStore : VaultFileStore {

    private val files = mutableMapOf<String, ByteArray>()

    override suspend fun write(id: String, bytes: ByteArray): AppResult<Unit> {
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
