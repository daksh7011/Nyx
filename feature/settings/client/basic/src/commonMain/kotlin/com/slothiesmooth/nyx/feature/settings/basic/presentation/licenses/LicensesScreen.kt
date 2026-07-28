package com.slothiesmooth.nyx.feature.settings.basic.presentation.licenses

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import com.slothiesmooth.nyx.feature.settings.basic.domain.nyxLicenses

/** Renders the hand-maintained [nyxLicenses] list, opening each entry's URL via [LocalUriHandler]. */
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    LicensesContent(entries = nyxLicenses, onOpenUrl = { url -> uriHandler.openUri(url) }, onBack = onBack)
}
