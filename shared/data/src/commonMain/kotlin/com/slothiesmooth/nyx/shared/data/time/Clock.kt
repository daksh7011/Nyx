package com.slothiesmooth.nyx.shared.data.time

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Injected wall clock. Direct `kotlin.time.Clock.System` / `kotlinx.datetime` system access is
 * banned everywhere except [SystemClock]; all other code depends on this interface so time is
 * controllable in tests (see `FakeClock` in `:shared:test-support`).
 */
interface Clock {
    fun now(): Instant
    fun today(): LocalDate
    fun zone(): TimeZone
    fun nowLocal(): LocalDateTime
}

/**
 * Shared projection helper so [SystemClock], `FakeClock`, and tests derive local time identically.
 */
fun Instant.toLocalDateTimeIn(zone: TimeZone): LocalDateTime = toLocalDateTime(zone)
