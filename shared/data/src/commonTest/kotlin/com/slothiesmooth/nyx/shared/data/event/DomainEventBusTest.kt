package com.slothiesmooth.nyx.shared.data.event

import com.slothiesmooth.nyx.shared.data.id.StegoImageId
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DomainEventBusTest {

    @Test
    fun `emit delivers the event to an active collector`() = runTest {
        val bus = DefaultDomainEventBus()
        val stored = StegoImageId("img-1")
        val collected = async(UnconfinedTestDispatcher(testScheduler)) { bus.events.first() }
        runCurrent() // let the collector subscribe before we emit

        bus.emit(DomainEvent.StegoImageStored(stored))

        assertEquals(DomainEvent.StegoImageStored(stored), collected.await())
    }

    @Test
    fun `emit without subscribers does not suspend`() = runTest {
        val bus = DefaultDomainEventBus()
        // replay = 0 with a 64-slot buffer means emit returns immediately when nobody listens;
        // reaching the assertion proves there was no deadlock.
        bus.emit(DomainEvent.VaultWiped)
        assertEquals(0, bus.events.replayCache.size)
    }
}
