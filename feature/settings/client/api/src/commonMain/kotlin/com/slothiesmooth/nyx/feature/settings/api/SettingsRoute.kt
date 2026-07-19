package com.slothiesmooth.nyx.feature.settings.api

import kotlinx.serialization.Serializable

/** The settings route (a bottom-nav tab). */
@Serializable
data object SettingsRoute

/** The open-source licenses sub-route, pushed from the settings screen. */
@Serializable
data object SettingsLicensesRoute
