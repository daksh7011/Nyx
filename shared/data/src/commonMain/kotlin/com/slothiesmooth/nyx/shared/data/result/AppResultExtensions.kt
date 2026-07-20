package com.slothiesmooth.nyx.shared.data.result

/**
 * Maps the success value, leaving an [AppResult.Err] untouched (returned as-is, so identity is
 * preserved for the error branch).
 */
inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Ok -> AppResult.Ok(transform(value))
    is AppResult.Err -> this
}

/**
 * Returns the success value, or `null` if this is an [AppResult.Err].
 */
fun <T> AppResult<T>.getOrNull(): T? = when (this) {
    is AppResult.Ok -> value
    is AppResult.Err -> null
}
