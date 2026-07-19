package com.slothiesmooth.nyx.feature.settings.basic.presentation

import com.slothiesmooth.nyx.feature.settings.basic.resources.Res
import com.slothiesmooth.nyx.feature.settings.basic.resources.settings_version_label
import com.slothiesmooth.nyx.shared.presentation.text.UiText

/** Combines the app name, [versionName], and [platformName] into the About row's single-line label. */
fun versionLabel(versionName: String, platformName: String): UiText =
    UiText.res(Res.string.settings_version_label, versionName, platformName)
