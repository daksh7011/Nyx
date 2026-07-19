package com.slothiesmooth.nyx.feature.settings.basic.presentation

import com.slothiesmooth.nyx.feature.settings.basic.resources.Res
import com.slothiesmooth.nyx.feature.settings.basic.resources.settings_version_label
import com.slothiesmooth.nyx.shared.presentation.text.UiText
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsLabelsTest {
    @Test
    fun `version label references the version-label resource with version and platform args`() {
        assertEquals(
            UiText.res(Res.string.settings_version_label, "1.0.0", "Android"),
            versionLabel(versionName = "1.0.0", platformName = "Android"),
        )
    }
}
