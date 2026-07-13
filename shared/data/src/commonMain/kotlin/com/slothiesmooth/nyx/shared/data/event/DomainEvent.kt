package com.slothiesmooth.nyx.shared.data.event

import com.slothiesmooth.nyx.shared.data.id.StegoImageId

/**
 * Cross-feature notifications. The [DomainEventBus] is the ONLY channel features use to react to
 * each other's state changes (no feature-to-feature api dependencies).
 */
sealed interface DomainEvent {
    data class StegoImageStored(val id: StegoImageId) : DomainEvent
    data class StegoImageArchived(val id: StegoImageId) : DomainEvent
    data class StegoImageRestored(val id: StegoImageId) : DomainEvent
    data class StegoImageDeleted(val id: StegoImageId) : DomainEvent
    data object VaultWiped : DomainEvent
}
