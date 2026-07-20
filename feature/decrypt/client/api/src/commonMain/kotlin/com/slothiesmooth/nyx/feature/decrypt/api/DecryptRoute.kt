package com.slothiesmooth.nyx.feature.decrypt.api

import kotlinx.serialization.Serializable

/** The decrypt route (a bottom-nav tab); [imageId] preselects a vault image, null = fresh pick. */
@Serializable
data class DecryptRoute(val imageId: String? = null)
