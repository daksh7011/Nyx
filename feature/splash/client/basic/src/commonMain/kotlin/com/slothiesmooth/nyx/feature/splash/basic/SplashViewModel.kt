package com.slothiesmooth.nyx.feature.splash.basic

import com.slothiesmooth.nyx.shared.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.delay

private const val SPLASH_DWELL_MS = 1_100L

/** Holds the splash on-screen for a short brand dwell, then flips [SplashState.ready]. */
class SplashViewModel(
    private val mutableState: SplashMutableState,
) : BaseViewModel() {

    val state: SplashState = mutableState

    override fun doBind() {
        startDwell()
    }

    /**
     * The dwell timer. Runs on [ui] (`Dispatchers.Main`) rather than [async] so a test can fast-forward
     * it via the main test dispatcher's virtual clock; [async]'s `Dispatchers.Default` is a real
     * background dispatcher the test scheduler cannot advance.
     */
    fun startDwell() = ui("splashDwell") {
        delay(SPLASH_DWELL_MS)
        withState { mutableState.ready = true }
    }
}
