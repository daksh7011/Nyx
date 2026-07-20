package com.slothiesmooth.nyx.shared.data.result

/**
 * Result of an operation that can fail with a typed [AppError]. Repository/source writes return
 * this; reads return `Flow`.
 */
sealed interface AppResult<out T> {
    data class Ok<T>(val value: T) : AppResult<T>
    data class Err(val cause: AppError) : AppResult<Nothing>
}
