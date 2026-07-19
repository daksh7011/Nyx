package com.slothiesmooth.nyx.feature.vault.api

import com.slothiesmooth.nyx.feature.common.api.routeNameOf
import kotlin.test.Test
import kotlin.test.assertEquals

class VaultRouteTest {

    @Test
    fun `routeNameOf resolves the fully qualified VaultRoute serial name`() {
        assertEquals(
            "com.slothiesmooth.nyx.feature.vault.api.VaultRoute",
            routeNameOf<VaultRoute>(),
        )
    }

    @Test
    fun `routeNameOf resolves the fully qualified VaultDetailRoute serial name`() {
        assertEquals(
            "com.slothiesmooth.nyx.feature.vault.api.VaultDetailRoute",
            routeNameOf<VaultDetailRoute>(),
        )
    }
}
