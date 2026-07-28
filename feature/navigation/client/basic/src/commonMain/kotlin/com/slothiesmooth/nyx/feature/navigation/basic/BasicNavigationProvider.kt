package com.slothiesmooth.nyx.feature.navigation.basic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.slothiesmooth.nyx.designlibrary.models.NxBottomNavItem
import com.slothiesmooth.nyx.designlibrary.organisms.NxBottomNav
import com.slothiesmooth.nyx.feature.common.api.BaseFeatureProvider
import com.slothiesmooth.nyx.feature.common.api.FeatureContext
import com.slothiesmooth.nyx.feature.common.api.LocalFeatureBottomBar
import com.slothiesmooth.nyx.feature.navigation.api.NavItem
import com.slothiesmooth.nyx.feature.navigation.api.NavigationFeature
import com.slothiesmooth.nyx.feature.navigation.basic.models.NavBarState
import com.slothiesmooth.nyx.shared.presentation.util.asString
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow

/** Renders the shell bottom navigation from client-supplied [NavItem]s. Has no route of its own. */
class BasicNavigationProvider : BaseFeatureProvider(), NavigationFeature {

    private val itemsState = MutableStateFlow<ImmutableList<NavItem>>(persistentListOf())

    override fun setItems(items: ImmutableList<NavItem>) {
        itemsState.value = items
    }

    @Composable
    override fun onProvideContent(context: FeatureContext, content: @Composable () -> Unit) {
        CompositionLocalProvider(LocalFeatureBottomBar provides { BottomBar(context) }) {
            content()
        }
    }

    @Composable
    private fun BottomBar(context: FeatureContext) {
        val items by itemsState.collectAsState()
        val state = remember(items) { NavBarState.from(items) }
        if (state.selectedIndex < 0) return
        NxBottomNav(
            items = state.items.toBottomNavItems(),
            selectedIndex = state.selectedIndex,
            // Re-tapping the active tab must not re-navigate: it would reload the screen and lose any
            // in-progress input (e.g. a half-filled encrypt wizard).
            onSelect = { index -> if (index != state.selectedIndex) context.setDestination(items[index].route) },
        )
    }

    @Composable
    private fun ImmutableList<NavItem>.toBottomNavItems(): ImmutableList<NxBottomNavItem> {
        val resolved = mutableListOf<NxBottomNavItem>()
        for (item in this) {
            resolved.add(NxBottomNavItem(icon = item.icon, label = item.label.asString()))
        }
        return resolved.toImmutableList()
    }
}
