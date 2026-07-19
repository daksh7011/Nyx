package com.slothiesmooth.nyx.feature.common.api

import androidx.compose.runtime.Composable
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

private const val FLOOD_COUNT = 10_000

class BaseFeatureProviderTest {

    private class TestProvider : BaseFeatureProvider() {
        @Composable
        override fun onProvideContent(context: FeatureContext, content: @Composable () -> Unit) = Unit

        fun emit(action: Action): Boolean = onSendAction(action)

        object Ping : Action
    }

    @Test
    fun `onSendAction never drops actions thanks to an unbounded buffer`() = runTest {
        val provider = TestProvider()
        repeat(FLOOD_COUNT) { assertTrue(provider.emit(TestProvider.Ping)) }
    }
}
