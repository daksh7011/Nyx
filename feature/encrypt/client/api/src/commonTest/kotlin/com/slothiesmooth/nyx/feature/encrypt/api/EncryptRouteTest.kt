package com.slothiesmooth.nyx.feature.encrypt.api

import com.slothiesmooth.nyx.feature.common.api.routeNameOf
import kotlin.test.Test
import kotlin.test.assertEquals

class EncryptRouteTest {

    @Test
    fun `routeNameOf resolves the fully qualified EncryptRoute serial name`() {
        assertEquals(
            "com.slothiesmooth.nyx.feature.encrypt.api.EncryptRoute",
            routeNameOf<EncryptRoute>(),
        )
    }
}
