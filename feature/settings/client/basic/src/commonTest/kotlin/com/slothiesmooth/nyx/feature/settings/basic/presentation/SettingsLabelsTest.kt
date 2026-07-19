package com.slothiesmooth.nyx.feature.settings.basic.presentation

import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsLabelsTest {
    @Test
    fun `version label combines app name version and platform`() {
        assertEquals("Nyx 1.0.0 · Android", versionLabel(versionName = "1.0.0", platformName = "Android"))
    }
}
