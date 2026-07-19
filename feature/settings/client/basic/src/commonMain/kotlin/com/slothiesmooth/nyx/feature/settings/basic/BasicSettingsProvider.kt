package com.slothiesmooth.nyx.feature.settings.basic

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.slothiesmooth.nyx.designlibrary.molecules.NxEmptyState
import com.slothiesmooth.nyx.designlibrary.tokens.NxIconKind
import com.slothiesmooth.nyx.feature.common.api.BaseFeatureProvider
import com.slothiesmooth.nyx.feature.common.api.FeatureContext
import com.slothiesmooth.nyx.feature.settings.api.SettingsFeature
import com.slothiesmooth.nyx.feature.settings.api.SettingsRoute

/**
 * Placeholder settings provider. Its one live action opens the change-theme screen (route injected as
 * `Any` by the app module — settings imports no other feature api). Plan 06 replaces the screen body.
 */
class BasicSettingsProvider(
    private val changeThemeRoute: Any,
) : BaseFeatureProvider(), SettingsFeature {

    @Composable
    override fun onProvideContent(context: FeatureContext, content: @Composable () -> Unit) = content()

    override fun onProvideNavigation(context: FeatureContext, builder: NavGraphBuilder) {
        builder.composable<SettingsRoute> {
            NxEmptyState(
                icon = NxIconKind.Settings,
                title = "Settings",
                body = "About, licenses, and vault controls are coming soon.",
                ctaText = "Change theme",
                onCta = { onSendAction(OpenTheme) },
            )
        }
    }

    override suspend fun onReceiveAction(action: BaseFeatureProvider.Action, context: FeatureContext) {
        if (action is OpenTheme) context.pushDestination(changeThemeRoute)
    }

    private data object OpenTheme : BaseFeatureProvider.Action
}
