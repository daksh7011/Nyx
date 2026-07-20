package com.slothiesmooth.nyx.feature.settings.api

import com.slothiesmooth.nyx.feature.common.api.routeNameOf
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsRouteTest {

    @Test
    fun `routeNameOf resolves the fully qualified SettingsRoute serial name`() {
        assertEquals(
            "com.slothiesmooth.nyx.feature.settings.api.SettingsRoute",
            routeNameOf<SettingsRoute>(),
        )
    }
}
