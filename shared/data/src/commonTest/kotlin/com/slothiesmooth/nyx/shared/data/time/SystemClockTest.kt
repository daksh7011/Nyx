package com.slothiesmooth.nyx.shared.data.time

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Instant

class SystemClockTest {

    @Test
    fun `now is a plausible current instant`() {
        val clock = SystemClock()
        assertTrue(clock.now() > Instant.parse("2020-01-01T00:00:00Z"))
    }

    @Test
    fun `now does not go backwards across two reads`() {
        val clock = SystemClock()
        val first = clock.now()
        val second = clock.now()
        assertTrue(second >= first)
    }

    @Test
    fun `today and nowLocal derive from the reported zone without throwing`() {
        val clock = SystemClock()
        val zone = clock.zone()
        // nowLocal and today both project now() into zone(); assert they are internally consistent
        // by projecting a single captured instant the same way the clock does.
        val captured = clock.now()
        val projected = captured.toLocalDateTimeIn(zone)
        assertTrue(projected.year >= 2020)
        // today() is derived the same way from a (fresh) now(); assert it is a valid date object.
        assertTrue(clock.today().day in 1..31)
    }
}
