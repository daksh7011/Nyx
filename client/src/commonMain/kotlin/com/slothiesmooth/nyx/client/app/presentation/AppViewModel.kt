package com.slothiesmooth.nyx.client.app.presentation

import com.slothiesmooth.nyx.client.resources.Res
import com.slothiesmooth.nyx.client.resources.nav_decrypt
import com.slothiesmooth.nyx.client.resources.nav_encrypt
import com.slothiesmooth.nyx.client.resources.nav_settings
import com.slothiesmooth.nyx.client.resources.nav_vault
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.feature.common.api.routeNameOf
import com.slothiesmooth.nyx.feature.decrypt.api.DecryptRoute
import com.slothiesmooth.nyx.feature.encrypt.api.EncryptRoute
import com.slothiesmooth.nyx.feature.navigation.api.NavItem
import com.slothiesmooth.nyx.feature.navigation.api.NavigationFeature
import com.slothiesmooth.nyx.feature.settings.api.SettingsRoute
import com.slothiesmooth.nyx.feature.splash.api.SplashRoute
import com.slothiesmooth.nyx.feature.vault.api.VaultRoute
import com.slothiesmooth.nyx.shared.presentation.text.UiText
import com.slothiesmooth.nyx.shared.presentation.viewmodel.BaseViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

private data class Tab(val route: Any, val routeName: String, val label: UiText, val icon: NxIconKind)

/** The shell view model: fixed start destination and the bottom-nav tab set with live selection. */
class AppViewModel(
    private val navigationFeature: NavigationFeature,
) : BaseViewModel() {

    val startDestination: Any = SplashRoute

    private val tabs: List<Tab> = listOf(
        Tab(VaultRoute, routeNameOf<VaultRoute>(), UiText.res(Res.string.nav_vault), NxIconKind.Vault),
        Tab(EncryptRoute, routeNameOf<EncryptRoute>(), UiText.res(Res.string.nav_encrypt), NxIconKind.Lock),
        Tab(DecryptRoute(), routeNameOf<DecryptRoute>(), UiText.res(Res.string.nav_decrypt), NxIconKind.Unlock),
        Tab(SettingsRoute, routeNameOf<SettingsRoute>(), UiText.res(Res.string.nav_settings), NxIconKind.Settings),
    )

    override fun doInit() {
        refreshNavItems(currentRouteName = null)
    }

    /** Rebuilds the tab items with [currentRouteName] highlighted, and hands them to the nav feature. */
    fun refreshNavItems(currentRouteName: String?) {
        val active = currentRouteName?.substringBefore('/')?.substringBefore('?')
        navigationFeature.setItems(navItems(active))
    }

    private fun navItems(activeRouteName: String?): ImmutableList<NavItem> = tabs.map { tab ->
        NavItem(
            route = tab.route,
            label = tab.label,
            icon = tab.icon,
            selected = tab.routeName == activeRouteName,
        )
    }.toImmutableList()
}
