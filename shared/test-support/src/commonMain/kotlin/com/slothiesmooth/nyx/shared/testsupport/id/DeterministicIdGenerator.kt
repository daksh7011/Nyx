package com.slothiesmooth.nyx.shared.testsupport.id

import com.slothiesmooth.nyx.shared.data.id.IdGenerator

/**
 * Deterministic [IdGenerator] for tests: emits `"$prefix-$n"` starting at [counter], incrementing
 * on each call, so generated ids are stable and readable in assertions.
 */
class DeterministicIdGenerator(
    private val prefix: String = "id",
    counter: Int = 0,
) : IdGenerator {
    private var next = counter
    override fun newId(): String = "$prefix-${next++}"
}
