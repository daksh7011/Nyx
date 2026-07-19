package com.slothiesmooth.nyx.feature.theme.basic

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.feature.common.api.FeatureContext
import com.slothiesmooth.nyx.feature.common.koin.KoinFeatureProvider
import com.slothiesmooth.nyx.feature.common.koin.koinFeatureViewModel
import com.slothiesmooth.nyx.feature.theme.api.ThemeConfig
import com.slothiesmooth.nyx.feature.theme.api.ThemeFeature
import com.slothiesmooth.nyx.feature.theme.api.ThemeMode
import com.slothiesmooth.nyx.feature.theme.api.ThemeRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf

private val DefaultConfig = ThemeConfig(ThemeMode.System, NxPalette.DefaultDark, NxPalette.DefaultLight)

/** The theme feature: persists selections through [ThemeRepository] and hosts the change-theme route. */
class BasicThemeProvider(
    private val repository: ThemeRepository,
    scope: CoroutineScope,
) : KoinFeatureProvider(), ThemeFeature {

    override val theme: StateFlow<ThemeConfig> =
        repository.observeConfig().stateIn(scope, SharingStarted.Eagerly, DefaultConfig)

    override suspend fun setMode(mode: ThemeMode) = repository.setMode(mode)

    override suspend fun setPalette(palette: NxPalette) = repository.setPalette(palette)

    @Composable
    override fun onProvideContent(context: FeatureContext, content: @Composable () -> Unit) {
        withDI { content() }
    }

    override fun onProvideNavigation(context: FeatureContext, builder: NavGraphBuilder) {
        builder.composable<ThemeRoute> {
            withDI { ChangeThemeScreen(viewModel = koinFeatureViewModel()) }
        }
    }

    override fun Module.onProvideDI() {
        single<ThemeFeature> { this@BasicThemeProvider }
        factoryOf(::ChangeThemeMutableState)
        viewModelOf(::ThemeViewModel)
    }
}
