package com.slothiesmooth.nyx.shared.presentation.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * Coroutine-launching foundation ported from the pawdex/Baro `shared:presentation`. Coroutines are
 * launched through a named-job map so a second launch with the same `id` is skipped while the first
 * is in flight, unless `force = true`, which cancels and replaces it. [BaseViewModel] adds the
 * Compose lifecycle wiring on top of these primitives.
 */
abstract class CoroutineViewModel : ViewModel() {

    private val jobs = mutableMapOf<String, Job>()

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
    ): Job? = jobs.launchDeduped(
        scope = viewModelScope,
        id = id,
        force = force,
        context = Dispatchers.Main,
        block = block,
    )

    /** Launches [block] on the default dispatcher under the named-job dedup keyed by [id]. */
    protected fun async(
        id: String,
        force: Boolean = false,
        block: suspend CoroutineScope.() -> Unit,
    ): Job? = jobs.launchDeduped(
        scope = viewModelScope,
        id = id,
        force = force,
        context = Dispatchers.Default,
        block = block,
    )
}

/**
 * Lifecycle-aware ViewModel base. `bind()` wires the Compose lifecycle to the
 * `doInit`/`doBind`/`doResume`/`doPause`/`doDispose` hooks, while the coroutine primitives are
 * inherited from [CoroutineViewModel].
 *
 * Reference: https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-lifecycle.html
 */
@Immutable
abstract class BaseViewModel : CoroutineViewModel() {

    private var initialized = false

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

/**
 * Launches [block] on [scope] under a named-job dedup keyed by [id]. A second launch with the same
 * [id] is skipped (returns `null`) while the first is still active, unless [force] is `true`, which
 * cancels the in-flight job and replaces it. The started [Job] is recorded back into the receiver
 * map so subsequent calls can observe it.
 */
private fun MutableMap<String, Job>.launchDeduped(
    scope: CoroutineScope,
    id: String,
    force: Boolean,
    context: CoroutineContext,
    block: suspend CoroutineScope.() -> Unit,
): Job? {
    val existing = this[id]
    return when {
        force -> {
            existing?.cancel()
            scope.launch(context = context, block = block).also { this[id] = it }
        }

        existing == null || existing.isCompleted -> {
            scope.launch(context = context, block = block).also { this[id] = it }
        }

        else -> null
    }
}
