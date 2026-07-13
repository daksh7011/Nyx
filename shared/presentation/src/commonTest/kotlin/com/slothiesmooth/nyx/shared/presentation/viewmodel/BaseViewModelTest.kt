package com.slothiesmooth.nyx.shared.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class ProbeViewModel : BaseViewModel() {
    var marks = 0
        private set
    var counter by mutableStateOf(0)
        private set

    fun mark() {
        marks++
    }

    fun runUi(id: String, force: Boolean = false, block: suspend CoroutineScope.() -> Unit): Job? =
        ui(id, force, block)

    fun runAsync(id: String, force: Boolean = false, block: suspend CoroutineScope.() -> Unit): Job? =
        async(id, force, block)

    fun bumpViaWithState() = withState { counter++ }
}

@OptIn(ExperimentalCoroutinesApi::class)
class BaseViewModelTest {

    @BeforeTest
    fun installMain() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun removeMain() {
        Dispatchers.resetMain()
    }

    @Test
    fun `ui with the same id is deduplicated while in flight`() = runTest {
        val vm = ProbeViewModel()
        val gate = CompletableDeferred<Unit>()

        val first = vm.runUi("load") {
            vm.mark()
            gate.await()
        }
        val second = vm.runUi("load") {
            vm.mark()
            gate.await()
        }

        assertNotNull(first)
        assertNull(second) // skipped: first is still active
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, vm.marks) // the block ran exactly once
    }

    @Test
    fun `ui with force cancels the in-flight job and runs the replacement`() = runTest {
        val vm = ProbeViewModel()
        val gate = CompletableDeferred<Unit>()

        val first = vm.runUi("load") {
            gate.await()
            vm.mark()
        }
        runCurrent() // let first start and suspend at the gate
        val second = vm.runUi("load", force = true) { vm.mark() }

        assertNotNull(second)
        assertTrue(first!!.isCancelled)
        advanceUntilIdle()
        assertEquals(1, vm.marks) // only the replacement marked; the cancelled job never reached mark()
    }

    @Test
    fun `async returns the running job and dedups by id`() = runTest {
        val vm = ProbeViewModel()
        val gate = CompletableDeferred<Unit>()

        val first = vm.runAsync("sync") { gate.await() }
        val second = vm.runAsync("sync") { gate.await() }

        assertNotNull(first)
        assertNull(second)
        gate.complete(Unit)
        first.join()
    }

    @Test
    fun `withState mutation is visible after the dispatcher drains`() = runTest {
        val vm = ProbeViewModel()

        vm.bumpViaWithState()
        advanceUntilIdle()

        assertEquals(1, vm.counter)
    }
}
