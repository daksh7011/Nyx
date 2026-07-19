package com.slothiesmooth.nyx.feature.common.api

import kotlinx.serialization.serializer

/**
 * The serialized route name the navigation library stores in `NavDestination.route` for a
 * no-argument type-safe route. Computed via the reified serializer at the call site (compile-time
 * type known), never from a `route: Any` value at runtime — the latter needs InternalSerializationApi
 * and is ambiguous on wasmJs.
 */
inline fun <reified T : Any> routeNameOf(): String = serializer<T>().descriptor.serialName
