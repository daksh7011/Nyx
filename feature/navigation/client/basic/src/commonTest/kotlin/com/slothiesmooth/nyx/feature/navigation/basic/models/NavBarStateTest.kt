package com.slothiesmooth.nyx.feature.navigation.basic.models

import com.slothiesmooth.nyx.designlibrary.models.NxBottomNavItem
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.feature.navigation.api.NavItem
import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NavBarStateTest {

    @Test
    fun `maps each nav item to its display item in order and selects the active one`() {
        val items = persistentListOf(
            NavItem(route = VaultRoute, label = "Vault", icon = NxIconKind.Vault, selected = false),
            NavItem(route = EncryptRoute, label = "Encrypt", icon = NxIconKind.Lock, selected = true),
            NavItem(route = SettingsRoute, label = "Settings", icon = NxIconKind.Settings, selected = false),
        )

        val state = NavBarState.from(items)

        assertEquals(
            persistentListOf(
                NxBottomNavItem(icon = NxIconKind.Vault, label = "Vault"),
                NxBottomNavItem(icon = NxIconKind.Lock, label = "Encrypt"),
                NxBottomNavItem(icon = NxIconKind.Settings, label = "Settings"),
            ),
            state.items,
        )
        assertEquals(1, state.selectedIndex)
    }

    @Test
    fun `reports no selection when nothing is selected so the bar can hide`() {
        val items = persistentListOf(
            NavItem(route = VaultRoute, label = "Vault", icon = NxIconKind.Vault, selected = false),
            NavItem(route = EncryptRoute, label = "Encrypt", icon = NxIconKind.Lock, selected = false),
        )

        val state = NavBarState.from(items)

        assertTrue(state.selectedIndex < 0, "expected a negative selectedIndex, was ${state.selectedIndex}")
    }

    @Test
    fun `an empty item list yields no selection`() {
        val state = NavBarState.from(persistentListOf())

        assertTrue(state.items.isEmpty())
        assertTrue(state.selectedIndex < 0, "expected a negative selectedIndex, was ${state.selectedIndex}")
    }

    private companion object {
        val VaultRoute = Any()
        val EncryptRoute = Any()
        val SettingsRoute = Any()
    }
}
