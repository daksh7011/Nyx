package com.slothiesmooth.nyx.feature.settings.api

import kotlinx.serialization.Serializable

/** The settings route (a bottom-nav tab). */
@Serializable
data object SettingsRoute

/** The open-source-licenses screen route, reached from settings. */
@Serializable
data object SettingsLicensesRoute
