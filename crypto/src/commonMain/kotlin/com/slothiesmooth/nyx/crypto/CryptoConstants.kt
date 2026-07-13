package com.slothiesmooth.nyx.crypto

internal const val SALT_SIZE_BYTES = 16
internal const val NONCE_SIZE_BYTES = 12
internal const val GCM_TAG_SIZE_BYTES = 16
internal const val PBKDF2_ITERATIONS = 600_000
internal const val KEY_SIZE_BITS = 256
internal const val MIN_BLOB_SIZE_BYTES = SALT_SIZE_BYTES + NONCE_SIZE_BYTES + GCM_TAG_SIZE_BYTES

internal const val REASON_MALFORMED_BASE64 = "blob is not valid Base64"
internal const val REASON_TOO_SHORT = "blob is shorter than salt + nonce + tag (44 bytes)"
