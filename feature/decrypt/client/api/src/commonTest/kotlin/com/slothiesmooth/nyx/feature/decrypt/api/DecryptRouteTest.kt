package com.slothiesmooth.nyx.feature.decrypt.api

import com.slothiesmooth.nyx.feature.common.api.routeNameOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DecryptRouteTest {

    @Test
    fun `routeNameOf resolves the fully qualified DecryptRoute serial name`() {
        assertEquals(
            "com.slothiesmooth.nyx.feature.decrypt.api.DecryptRoute",
            routeNameOf<DecryptRoute>(),
        )
    }

    @Test
    fun `imageId defaults to null for a fresh pick`() {
        assertNull(DecryptRoute().imageId)
    }
}
