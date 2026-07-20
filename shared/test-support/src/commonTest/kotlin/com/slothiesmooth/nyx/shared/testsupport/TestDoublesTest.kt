package com.slothiesmooth.nyx.shared.testsupport

import com.slothiesmooth.nyx.shared.testsupport.id.DeterministicIdGenerator
import com.slothiesmooth.nyx.shared.testsupport.time.FakeClock
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class TestDoublesTest {

    @Test
    fun `fake clock returns the fixed instant`() {
        val fixed = Instant.parse("2026-07-13T00:00:00Z")
        assertEquals(fixed, FakeClock(fixed).now())
    }

    @Test
    fun `advance moves the fixed instant forward`() {
        val start = Instant.parse("2026-07-13T00:00:00Z")
        val clock = FakeClock(start)
        clock.advance(3.hours)
        assertEquals(start + 3.hours, clock.now())
    }

    @Test
    fun `today derives from the fixed instant in the configured zone`() {
        val clock = FakeClock(Instant.parse("2026-07-13T10:00:00Z"))
        assertEquals(LocalDate(2026, 7, 13), clock.today())
        assertEquals(2026, clock.nowLocal().year)
    }

    @Test
    fun `deterministic id generator emits sequential prefixed ids`() {
        val generator = DeterministicIdGenerator(prefix = "img", counter = 0)
        assertEquals("img-0", generator.newId())
        assertEquals("img-1", generator.newId())
        assertEquals("img-2", generator.newId())
    }
}
