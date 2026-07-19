package com.slothiesmooth.nyx.feature.settings.basic.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import com.slothiesmooth.nyx.feature.common.koin.koinFeatureViewModel

private const val NYX_REPO_URL = "https://github.com/slothiesmooth/nyx"

/** Resolves [SettingsViewModel] from the isolated graph and renders [SettingsContent] over its state. */
@Composable
fun SettingsScreen(
    onOpenTheme: () -> Unit,
    onOpenLicenses: () -> Unit,
) {
    val viewModel = koinFeatureViewModel<SettingsViewModel>()
    val uriHandler = LocalUriHandler.current
    SettingsContent(
        state = viewModel.state,
        onOpenTheme = onOpenTheme,
        onOpenLicenses = onOpenLicenses,
        onOpenRepo = { uriHandler.openUri(NYX_REPO_URL) },
        onRequestWipe = viewModel::requestWipe,
        onConfirmWipe = viewModel::confirmWipe,
        onCancelWipe = viewModel::cancelWipe,
    )
}
