package com.slothiesmooth.nyx.feature.splash.basic

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.slothiesmooth.nyx.shared.presentation.state.MutableViewState
import com.slothiesmooth.nyx.shared.presentation.state.ViewState

/** Read-only splash state: [ready] flips true once the brand dwell elapses. */
interface SplashState : ViewState {
    val ready: Boolean
}

class SplashMutableState : MutableViewState(), SplashState {
    override var ready: Boolean by mutableStateOf(false)
}
