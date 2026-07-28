package com.slothiesmooth.nyx.shared.testsupport.time

import com.slothiesmooth.nyx.shared.data.time.Clock
import com.slothiesmooth.nyx.shared.data.time.toLocalDateTimeIn
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Deterministic [Clock] for tests: reports a [fixed] instant (default zone UTC) and derives local
 * time exactly as `SystemClock` does. [advance] moves the fixed instant forward.
 */
@OptIn(ExperimentalTime::class)
class FakeClock(
    var fixed: Instant,
    private val zone: TimeZone = TimeZone.UTC,
) : Clock {
    override fun now(): Instant = fixed
    override fun zone(): TimeZone = zone
    override fun nowLocal(): LocalDateTime = fixed.toLocalDateTimeIn(zone)
    override fun today(): LocalDate = nowLocal().date

    fun advance(by: Duration) {
        fixed += by
    }
}
