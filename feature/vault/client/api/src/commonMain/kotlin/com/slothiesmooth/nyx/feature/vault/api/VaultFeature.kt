package com.slothiesmooth.nyx.feature.vault.api

import com.slothiesmooth.nyx.feature.common.api.Feature
import kotlinx.coroutines.flow.Flow

/** Cross-feature handle to the vault. The active-count feed drives the empty-state gate elsewhere. */
interface VaultFeature : Feature {
    fun observeActiveCount(): Flow<Int>
}
