package com.slothiesmooth.nyx.feature.navigation.basic.models

import androidx.compose.runtime.Immutable
import com.slothiesmooth.nyx.feature.navigation.api.NavItem
import kotlinx.collections.immutable.ImmutableList

/**
 * Render-ready bottom-bar state derived from the client's [NavItem]s: the [items] to display (their
 * labels are resolved when the bar draws) and the [selectedIndex] of the active destination (< 0 when
 * nothing is selected, i.e. the bar is hidden).
 */
@Immutable
internal data class NavBarState(
    val items: ImmutableList<NavItem>,
    val selectedIndex: Int,
) {
    companion object {
        fun from(items: ImmutableList<NavItem>): NavBarState =
            NavBarState(items = items, selectedIndex = items.indexOfFirst { item -> item.selected })
    }
}
