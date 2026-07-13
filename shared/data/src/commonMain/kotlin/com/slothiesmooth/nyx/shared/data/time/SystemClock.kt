package com.slothiesmooth.nyx.shared.data.time

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.time.Clock as KotlinTimeClock

/**
 * Production [Clock] reading the system wall clock and default time zone. This is the ONLY place
 * `kotlin.time.Clock.System` may be referenced (global constraint).
 */
@OptIn(ExperimentalTime::class)
class SystemClock : Clock {
    override fun now(): Instant = KotlinTimeClock.System.now()
    override fun zone(): TimeZone = TimeZone.currentSystemDefault()
    override fun nowLocal(): LocalDateTime = now().toLocalDateTimeIn(zone())
    override fun today(): LocalDate = nowLocal().date
}
