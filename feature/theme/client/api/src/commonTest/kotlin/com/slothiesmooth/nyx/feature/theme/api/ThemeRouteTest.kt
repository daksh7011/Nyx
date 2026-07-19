package com.slothiesmooth.nyx.feature.theme.api

import com.slothiesmooth.nyx.feature.common.api.routeNameOf
import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeRouteTest {

    @Test
    fun `routeNameOf resolves the fully qualified ThemeRoute serial name`() {
        assertEquals(
            "com.slothiesmooth.nyx.feature.theme.api.ThemeRoute",
            routeNameOf<ThemeRoute>(),
        )
    }
}
