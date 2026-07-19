package com.slothiesmooth.nyx.feature.theme.basic

import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.feature.theme.api.ThemeFeature
import com.slothiesmooth.nyx.feature.theme.api.ThemeMode
import com.slothiesmooth.nyx.shared.presentation.viewmodel.BaseViewModel

/** Drives the change-theme screen: mirrors the feature's [ThemeFeature.theme] and forwards choices. */
class ThemeViewModel(
    private val mutableState: ChangeThemeMutableState,
    private val themeFeature: ThemeFeature,
) : BaseViewModel() {

    val state: ChangeThemeState = mutableState

    override fun doBind() {
        observeTheme()
    }

    private fun observeTheme() = async("observeTheme") {
        themeFeature.theme.collect { config ->
            withState {
                mutableState.mode = config.mode
                mutableState.darkPalette = config.darkPalette
                mutableState.lightPalette = config.lightPalette
            }
        }
    }

    fun onModeSelected(mode: ThemeMode) = async("setMode", force = true) { themeFeature.setMode(mode) }

    fun onPaletteSelected(palette: NxPalette) =
        async("setPalette", force = true) { themeFeature.setPalette(palette) }
}
