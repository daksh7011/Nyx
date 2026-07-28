package com.slothiesmooth.nyx.shared.data.source

import com.slothiesmooth.nyx.shared.data.result.AppResult

/**
 * PNG-byte store for stego images, keyed by id (FileKit vault directory on non-web; in-memory on
 * web). Metadata lives in [VaultSource].
 */
interface VaultFileStore {
    suspend fun write(id: String, bytes: ByteArray): AppResult<Unit>
    suspend fun read(id: String): AppResult<ByteArray>
    suspend fun delete(id: String): AppResult<Unit>
    suspend fun deleteAll(): AppResult<Unit>
}
