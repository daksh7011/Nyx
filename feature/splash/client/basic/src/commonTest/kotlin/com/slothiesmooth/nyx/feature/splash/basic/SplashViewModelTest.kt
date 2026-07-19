package com.slothiesmooth.nyx.feature.splash.basic

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val AFTER_DWELL_MS = 2_000L

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `becomes ready only after the brand dwell elapses`() = runTest {
        val viewModel = SplashViewModel(SplashMutableState())
        viewModel.startDwell()
        runCurrent()
        assertFalse(viewModel.state.ready)
        advanceTimeBy(AFTER_DWELL_MS)
        runCurrent()
        assertTrue(viewModel.state.ready)
    }
}
