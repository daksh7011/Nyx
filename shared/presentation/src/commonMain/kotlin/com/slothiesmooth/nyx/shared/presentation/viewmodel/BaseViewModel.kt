package com.slothiesmooth.nyx.shared.presentation.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * Lifecycle-aware ViewModel base ported from the pawdex/Baro `shared:presentation`. Coroutines launch
 * through a named-job map so a second launch with the same [id] is skipped (returns `null`) while the
 * first is in flight, unless `force = true`, which cancels the in-flight job and replaces it. `bind()`
 * wires the Compose lifecycle to the `doInit`/`doBind`/`doResume`/`doPause`/`doDispose` hooks.
 *
 * https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-lifecycle.html
 */
// TooManyFunctions is suppressed by explicit user decision: this is one cohesive base API (the
// coroutine primitives + the lifecycle hooks belong together); splitting it into two classes was
// rejected. This is the single sanctioned exception for this aggregator base class.
@Immutable
@Suppress("TooManyFunctions")
abstract class BaseViewModel : ViewModel() {

    private val jobs = mutableMapOf<String, Job>()
    private var initialized = false

    /** Takes a mutable snapshot and runs [block] within it on the main thread. */
    protected fun withState(block: () -> Unit) {
        viewModelScope.launch(Dispatchers.Main.immediate) {
            Snapshot.withMutableSnapshot(block)
        }
    }

    /** Launches [block] on the main dispatcher under the named-job dedup keyed by [id]. */
    protected fun ui(
        id: String,
        force: Boolean = false,
        block: suspend CoroutineScope.() -> Unit,
    ): Job? = launchDeduped(id = id, force = force, context = Dispatchers.Main, block = block)

    /** Launches [block] on the default dispatcher under the named-job dedup keyed by [id]. */
    protected fun async(
        id: String,
        force: Boolean = false,
        block: suspend CoroutineScope.() -> Unit,
    ): Job? = launchDeduped(id = id, force = force, context = Dispatchers.Default, block = block)

    private fun launchDeduped(
        id: String,
        force: Boolean,
        context: CoroutineContext,
        block: suspend CoroutineScope.() -> Unit,
    ): Job? {
        val existing = jobs[id]
        return when {
            force -> {
                existing?.cancel()
                viewModelScope.launch(context = context, block = block).also { jobs[id] = it }
            }

            existing == null || existing.isCompleted -> {
                viewModelScope.launch(context = context, block = block).also { jobs[id] = it }
            }

            else -> null
        }
    }

    protected open fun doInit() = Unit

    @Composable
    protected open fun DoBind() = Unit

    protected open fun doBind() = Unit

    protected open fun doResume() = Unit

    protected open fun doPause() = Unit

    protected open fun doDispose() = Unit

    /** Binds the ViewModel to the current Composable lifecycle. */
    @Composable
    fun bind() {
        DoBind()
        val owner = LocalLifecycleOwner.current
        LaunchedEffect(owner) {
            if (!initialized) {
                initialized = true
                doInit()
            }
            doBind()
            var initialRequest = true
            owner.lifecycle.currentStateFlow.collect { state ->
                when (state) {
                    Lifecycle.State.RESUMED -> {
                        if (!initialRequest) {
                            doResume()
                        }
                        initialRequest = false
                    }

                    Lifecycle.State.STARTED -> {
                        if (!initialRequest) {
                            doPause()
                        }
                    }

                    else -> Unit
                }
            }
        }
    }

    override fun onCleared() {
        doDispose()
    }
}
