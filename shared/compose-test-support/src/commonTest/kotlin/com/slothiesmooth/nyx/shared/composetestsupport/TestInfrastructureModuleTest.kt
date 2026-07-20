package com.slothiesmooth.nyx.shared.composetestsupport

import com.slothiesmooth.nyx.shared.data.event.DefaultDomainEventBus
import com.slothiesmooth.nyx.shared.data.event.DomainEventBus
import com.slothiesmooth.nyx.shared.data.id.IdGenerator
import com.slothiesmooth.nyx.shared.data.time.Clock
import com.slothiesmooth.nyx.shared.testsupport.id.DeterministicIdGenerator
import com.slothiesmooth.nyx.shared.testsupport.time.FakeClock
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertIs

class TestInfrastructureModuleTest {

    @Test
    fun `provides deterministic clock id generator and event bus`() {
        val koin = koinApplication { modules(testInfrastructureModule()) }.koin
        try {
            assertIs<FakeClock>(koin.get<Clock>())
            assertIs<DeterministicIdGenerator>(koin.get<IdGenerator>())
            assertIs<DefaultDomainEventBus>(koin.get<DomainEventBus>())
        } finally {
            koin.close()
        }
    }
}
