package com.slothiesmooth.nyx.shared.presentation.util

import androidx.compose.runtime.Composable
import com.slothiesmooth.nyx.shared.presentation.text.UiText
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/**
 * Resolves [UiText] to a String inside a Compose context, formatting any arguments.
 *
 * The spread is unavoidable: compose-resources exposes only `stringResource(StringResource, vararg Any)`
 * with no `List` overload, so forwarding a dynamic arg list requires it. The arrays are tiny (a
 * handful of format args), so the copy is negligible.
 */
@Composable
@Suppress("SpreadOperator")
fun UiText.asString(): String = when (this) {
    is UiText.Raw -> value
    is UiText.Resource -> stringResource(id, *args.toTypedArray())
}

/**
 * Resolves [UiText] to a String outside Compose (e.g. a share filename), formatting any arguments.
 *
 * Spread is unavoidable for the same reason as [asString]: `getString` is vararg-only.
 */
@Suppress("SpreadOperator")
suspend fun UiText.resolve(): String = when (this) {
    is UiText.Raw -> value
    is UiText.Resource -> getString(id, *args.toTypedArray())
}
