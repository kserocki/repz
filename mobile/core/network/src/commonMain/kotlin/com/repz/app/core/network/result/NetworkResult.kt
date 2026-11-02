package com.repz.app.core.network.result

/**
 * The outcome of a network call that can fail: typed, so every failure is a [NetworkError] instead
 * of an exception crossing the module boundary. This never becomes a domain result here: `core:domain`
 * must not learn transport concepts, so `core:data`/`core:sync` map this to their own domain errors.
 */
sealed interface NetworkResult<out T> {
    data class Success<out T>(
        val value: T,
    ) : NetworkResult<T>

    data class Failure(
        val error: NetworkError,
    ) : NetworkResult<Nothing>
}

inline fun <T, R> NetworkResult<T>.map(transform: (T) -> R): NetworkResult<R> =
    when (this) {
        is NetworkResult.Success -> NetworkResult.Success(transform(value))
        is NetworkResult.Failure -> this
    }

inline fun <T, R> NetworkResult<T>.fold(
    onSuccess: (T) -> R,
    onFailure: (NetworkError) -> R,
): R =
    when (this) {
        is NetworkResult.Success -> onSuccess(value)
        is NetworkResult.Failure -> onFailure(error)
    }

fun <T> NetworkResult<T>.getOrNull(): T? =
    when (this) {
        is NetworkResult.Success -> value
        is NetworkResult.Failure -> null
    }

fun NetworkResult<*>.errorOrNull(): NetworkError? =
    when (this) {
        is NetworkResult.Success -> null
        is NetworkResult.Failure -> error
    }
