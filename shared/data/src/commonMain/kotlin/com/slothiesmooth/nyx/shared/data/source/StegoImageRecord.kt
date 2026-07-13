package com.slothiesmooth.nyx.shared.data.source

/**
 * Database projection of a stored stego image's metadata. Timestamps are ISO-8601 strings;
 * [deletedAt] non-null marks a tombstone. Byte content lives in [VaultFileStore], not here.
 */
data class StegoImageRecord(
    val id: String,
    val name: String,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String?,
    val isArchived: Boolean,
)
