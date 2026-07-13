package com.slothiesmooth.nyx.shared.data.event

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Application-wide bus of [DomainEvent]s.
 */
interface DomainEventBus {
    val events: SharedFlow<DomainEvent>
    suspend fun emit(event: DomainEvent)
}

private const val EVENT_BUFFER_CAPACITY = 64

/**
 * Default bus: hot [MutableSharedFlow] with no replay (events are transient) and a 64-slot buffer
 * so emitters never suspend when no one is listening. Registered as an outer app singleton.
 */
class DefaultDomainEventBus : DomainEventBus {
    private val sink = MutableSharedFlow<DomainEvent>(
        replay = 0,
        extraBufferCapacity = EVENT_BUFFER_CAPACITY,
    )
    override val events: SharedFlow<DomainEvent> = sink.asSharedFlow()
    override suspend fun emit(event: DomainEvent) = sink.emit(event)
}
