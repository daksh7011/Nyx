package com.slothiesmooth.nyx.shared.data.result

/**
 * Closed set of failure reasons returned by repository writes and source operations.
 */
sealed interface AppError {
    data object NotFound : AppError
    data class Validation(val message: String) : AppError
    data class Storage(val message: String, val cause: Throwable? = null) : AppError
    data object Permission : AppError
    data class Conflict(val message: String) : AppError
}
