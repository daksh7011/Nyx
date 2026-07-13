package com.slothiesmooth.nyx.shared.data.id

/**
 * Produces opaque unique identifiers. The production implementation is [Uuid4IdGenerator];
 * tests use `DeterministicIdGenerator` from `:shared:test-support`.
 */
interface IdGenerator {
    fun newId(): String
}
