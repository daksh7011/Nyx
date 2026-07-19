package com.slothiesmooth.nyx.feature.theme.basic

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.slothiesmooth.nyx.designlibrary.tokens.NxPalette
import com.slothiesmooth.nyx.feature.theme.api.ThemeMode
import com.slothiesmooth.nyx.shared.presentation.state.MutableViewState
import com.slothiesmooth.nyx.shared.presentation.state.ViewState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/** Read-only change-theme state the screen observes. */
interface ChangeThemeState : ViewState {
    val mode: ThemeMode
    val darkPalette: NxPalette
    val lightPalette: NxPalette
    val palettes: ImmutableList<NxPalette>
}

/** Mutable backing state, owned by [ThemeViewModel] and supplied via Koin. */
class ChangeThemeMutableState : MutableViewState(), ChangeThemeState {
    override var mode: ThemeMode by mutableStateOf(ThemeMode.System)
    override var darkPalette: NxPalette by mutableStateOf(NxPalette.DefaultDark)
    override var lightPalette: NxPalette by mutableStateOf(NxPalette.DefaultLight)
    override val palettes: ImmutableList<NxPalette> = NxPalette.entries.toImmutableList()
}
