package com.slothiesmooth.nyx.feature.vault.basic.domain.model

import com.slothiesmooth.nyx.shared.data.id.StegoImageId
import kotlin.time.Instant

data class VaultImage(
    val id: StegoImageId,
    val name: String,
    val createdAt: Instant,
    val isArchived: Boolean,
)
