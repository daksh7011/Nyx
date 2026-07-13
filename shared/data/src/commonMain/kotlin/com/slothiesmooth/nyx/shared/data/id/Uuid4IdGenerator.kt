package com.slothiesmooth.nyx.shared.data.id

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * [IdGenerator] backed by a random (version 4) UUID from the Kotlin stdlib. `Uuid.random()`
 * renders as the canonical lowercase hex-dash form via `toString()`.
 */
@OptIn(ExperimentalUuidApi::class)
class Uuid4IdGenerator : IdGenerator {
    override fun newId(): String = Uuid.random().toString()
}
