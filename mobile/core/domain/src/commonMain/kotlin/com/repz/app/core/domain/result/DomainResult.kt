package com.repz.app.core.domain.result

/**
 * The outcome of a domain operation that can fail: typed, so every failure is a [DomainError]
 * subtype instead of an exception crossing a layer boundary.
 */
sealed interface DomainResult<out T, out E : DomainError> {
    data class Success<out T>(
        val value: T,
    ) : DomainResult<T, Nothing>

    data class Failure<out E : DomainError>(
        val error: E,
    ) : DomainResult<Nothing, E>
}

inline fun <T, E : DomainError, R> DomainResult<T, E>.map(transform: (T) -> R): DomainResult<R, E> =
    when (this) {
        is DomainResult.Success -> DomainResult.Success(transform(value))
        is DomainResult.Failure -> this
    }

inline fun <T, E : DomainError, R> DomainResult<T, E>.flatMap(transform: (T) -> DomainResult<R, E>): DomainResult<R, E> =
    when (this) {
        is DomainResult.Success -> transform(value)
        is DomainResult.Failure -> this
    }

inline fun <T, E : DomainError, R> DomainResult<T, E>.fold(
    onSuccess: (T) -> R,
    onFailure: (E) -> R,
): R =
    when (this) {
        is DomainResult.Success -> onSuccess(value)
        is DomainResult.Failure -> onFailure(error)
    }

fun <T> DomainResult<T, *>.getOrNull(): T? =
    when (this) {
        is DomainResult.Success -> value
        is DomainResult.Failure -> null
    }

fun <E : DomainError> DomainResult<*, E>.errorOrNull(): E? =
    when (this) {
        is DomainResult.Success -> null
        is DomainResult.Failure -> error
    }
