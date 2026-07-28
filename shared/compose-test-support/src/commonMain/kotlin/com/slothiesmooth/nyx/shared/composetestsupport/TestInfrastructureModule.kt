package com.slothiesmooth.nyx.shared.composetestsupport

import com.slothiesmooth.nyx.shared.data.event.DefaultDomainEventBus
import com.slothiesmooth.nyx.shared.data.event.DomainEventBus
import com.slothiesmooth.nyx.shared.data.id.IdGenerator
import com.slothiesmooth.nyx.shared.data.time.Clock
import com.slothiesmooth.nyx.shared.testsupport.id.DeterministicIdGenerator
import com.slothiesmooth.nyx.shared.testsupport.time.FakeClock
import org.koin.core.module.Module
import org.koin.dsl.module
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** Fixed instant used by the default test clock. */
@OptIn(ExperimentalTime::class)
private val FIXED_TEST_INSTANT: Instant = Instant.parse("2026-07-13T00:00:00Z")

/**
 * Koin module binding the platform-agnostic deterministic test doubles shared by feature UI tests:
 * a [FakeClock], a [DeterministicIdGenerator], and a fresh [DefaultDomainEventBus]. Feature tests
 * combine this with their own module (which supplies the in-memory DB / repositories).
 */
@OptIn(ExperimentalTime::class)
fun testInfrastructureModule(
    clock: Clock = FakeClock(FIXED_TEST_INSTANT),
    idGenerator: IdGenerator = DeterministicIdGenerator(),
): Module = module {
    single { clock }
    single { idGenerator }
    single<DomainEventBus> { DefaultDomainEventBus() }
}
