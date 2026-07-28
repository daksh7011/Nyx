package com.slothiesmooth.nyx.feature.navigation.basic.models

import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.feature.navigation.api.NavItem
import com.slothiesmooth.nyx.shared.presentation.text.UiText
import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NavBarStateTest {

    @Test
    fun `keeps the items in order and selects the active one`() {
        val items = persistentListOf(
            navItem(VaultRoute, "Vault", NxIconKind.Vault, selected = false),
            navItem(EncryptRoute, "Encrypt", NxIconKind.Lock, selected = true),
            navItem(SettingsRoute, "Settings", NxIconKind.Settings, selected = false),
        )

        val state = NavBarState.from(items)

        assertEquals(items, state.items)
        assertEquals(1, state.selectedIndex)
    }

    @Test
    fun `reports no selection when nothing is selected so the bar can hide`() {
        val items = persistentListOf(
            navItem(VaultRoute, "Vault", NxIconKind.Vault, selected = false),
            navItem(EncryptRoute, "Encrypt", NxIconKind.Lock, selected = false),
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

        fun navItem(route: Any, label: String, icon: NxIconKind, selected: Boolean): NavItem =
            NavItem(route = route, label = UiText.raw(label), icon = icon, selected = selected)
    }
}
