package com.slothiesmooth.nyx.feature.vault.api

import kotlinx.serialization.Serializable

/** The vault grid route (a bottom-nav tab). */
@Serializable
data object VaultRoute

/** The vault image detail route. */
@Serializable
data class VaultDetailRoute(val imageId: String)
