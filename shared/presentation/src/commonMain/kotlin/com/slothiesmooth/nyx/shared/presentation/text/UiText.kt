package com.slothiesmooth.nyx.shared.presentation.text

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.StringResource

/**
 * User-facing text that may be a plain string or a string-resource reference. It lets non-Compose
 * layers (ViewModels, domain) name text without resolving it, deferring the actual lookup to a
 * Compose context (the active locale). Resolve it with `UiText.asString()` (Compose) or
 * `UiText.resolve()` (suspend) — both in the `util` package.
 */
@Immutable
sealed interface UiText {

    /** Already-resolved or genuinely dynamic text (e.g. a filename); passed through verbatim. */
    @Immutable
    data class Raw(val value: String) : UiText

    /** A string resource plus any format arguments, resolved against the active locale later. */
    @Immutable
    data class Resource(val id: StringResource, val args: ImmutableList<Any>) : UiText

    companion object {
        /** Wraps an already-resolved or dynamic string. */
        fun raw(value: String): UiText = Raw(value)

        /** References a string resource, optionally with positional format arguments. */
        fun res(id: StringResource, vararg args: Any): UiText = Resource(id, args.toList().toImmutableList())
    }
}
