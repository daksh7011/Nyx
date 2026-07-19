package com.slothiesmooth.nyx.feature.common.api

import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.Flow

/**
 * The navigation surface handed to every feature. Route matching is by serialized route name
 * (String) rather than a destination id — wasm-safe, since `route::class.serializer()` is
 * `InternalSerializationApi` and ambiguous on wasmJs.
 */
@Stable
interface FeatureContext {
    /** Emits the current destination's serialized route name on every back-stack change. */
    fun getCurrentDestinationChanges(): Flow<String?>

    /** The current destination's serialized route name, or null before the graph is ready. */
    fun getCurrentDestination(): String?

    fun pushDestination(route: Any)
    fun setDestination(route: Any)
    fun replaceDestination(route: Any)
    fun restoreDestination(route: Any)
    fun popDestination()
}
