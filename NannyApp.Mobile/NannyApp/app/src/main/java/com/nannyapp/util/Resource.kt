package com.nannyapp.util

/**
 * Uniform wrapper for every repository call so the UI layer always has a
 * single, predictable way to render loading / success / error states
 * (see request #39 — friendly error handling, retry, never crash).
 */
sealed class Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String, val throwable: Throwable? = null, val code: Int? = null) : Resource<Nothing>()
    data object Loading : Resource<Nothing>()

    inline fun <R> map(transform: (T) -> R): Resource<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
        Loading -> Loading
    }

    val dataOrNull: T? get() = (this as? Success)?.data
}

/** Friendly, non-technical error copy shown to the user, per request #39. */
object ErrorMessages {
    const val NO_INTERNET = "You're offline. Check your connection and try again."
    const val SERVER_UNAVAILABLE = "Something went wrong on our end. Please try again shortly."
    const val UNAUTHORIZED = "Your session has expired. Please log in again."
    const val TIMEOUT = "That took too long. Please try again."
    const val GENERIC = "Something went wrong. Please try again."
    const val EMPTY = "Nothing to show here yet."
}
