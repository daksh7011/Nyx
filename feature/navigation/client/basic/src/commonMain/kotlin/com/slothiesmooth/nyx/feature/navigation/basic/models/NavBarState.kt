package com.slothiesmooth.nyx.feature.navigation.basic.models

import androidx.compose.runtime.Immutable
import com.slothiesmooth.nyx.designlibrary.models.NxBottomNavItem
import com.slothiesmooth.nyx.feature.navigation.api.NavItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * Render-ready bottom-bar state derived from the client's [NavItem]s: the display [items] and the
 * [selectedIndex] of the active destination (< 0 when nothing is selected, i.e. the bar is hidden).
 */
@Immutable
internal data class NavBarState(
    val items: ImmutableList<NxBottomNavItem>,
    val selectedIndex: Int,
) {
    companion object {
        fun from(items: ImmutableList<NavItem>): NavBarState = NavBarState(
            items = items.map { item -> NxBottomNavItem(icon = item.icon, label = item.label) }
                .toImmutableList(),
            selectedIndex = items.indexOfFirst { item -> item.selected },
        )
    }
}
