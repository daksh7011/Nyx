package com.slothiesmooth.nyx.shared.presentation.util

import androidx.compose.runtime.Composable
import com.slothiesmooth.nyx.shared.presentation.text.UiText
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/**
 * Resolves [UiText] to a String inside a Compose context, formatting any arguments. A [StringResource]
 * argument is itself resolved first (e.g. a localized month name embedded in a date pattern), so
 * nested resources compose cleanly.
 *
 * The spread is unavoidable: compose-resources exposes only vararg stringResource/getString with no
 * List overload. The arrays are tiny (a handful of format args), so the copy is negligible.
 */
@Composable
@Suppress("SpreadOperator")
fun UiText.asString(): String = when (this) {
    is UiText.Raw -> value
    is UiText.Resource -> {
        val formatArgs = args.toTypedArray()
        args.forEachIndexed { index, arg -> if (arg is StringResource) formatArgs[index] = stringResource(arg) }
        stringResource(id, *formatArgs)
    }
}

/**
 * Resolves [UiText] to a String outside Compose (e.g. a share filename), formatting any arguments and
 * any nested [StringResource]. Spread is unavoidable for the same reason as [asString].
 */
@Suppress("SpreadOperator")
suspend fun UiText.resolve(): String = when (this) {
    is UiText.Raw -> value
    is UiText.Resource -> {
        val formatArgs = args.toTypedArray()
        args.forEachIndexed { index, arg -> if (arg is StringResource) formatArgs[index] = getString(arg) }
        getString(id, *formatArgs)
    }
}
