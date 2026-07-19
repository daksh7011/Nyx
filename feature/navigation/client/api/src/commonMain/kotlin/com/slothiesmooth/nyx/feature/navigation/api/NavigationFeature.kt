package com.slothiesmooth.nyx.feature.navigation.api

import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.feature.common.api.Feature
import kotlinx.collections.immutable.ImmutableList

/**
 * One bottom-nav destination. [route] is the opaque target the app module supplies (never matched at
 * runtime — wasm-safe); [selected] is computed in `:client`, where the concrete route types are known.
 */
data class NavItem(
    val route: Any,
    val label: String,
    val icon: NxIconKind,
    val selected: Boolean,
)

/** Cross-feature handle to the shell bottom navigation. The app module pushes the current items. */
interface NavigationFeature : Feature {
    fun setItems(items: ImmutableList<NavItem>)
}
