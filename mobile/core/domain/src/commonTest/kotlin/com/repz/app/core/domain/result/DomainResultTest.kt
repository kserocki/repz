package com.repz.app.core.domain.result

import kotlin.test.Test
import kotlin.test.assertEquals

// DomainError/ValidationError are sealed in this module's commonMain, so a test error type must
// reuse one of their existing members rather than declaring a new subtype from commonTest.
private val testError = ValidationError.InvalidRpe

class DomainResultTest {
    @Test
    fun givenSuccess_whenMapped_thenTransformsTheValue() {
        val result: DomainResult<Int, ValidationError> = DomainResult.Success(2)

        val mapped = result.map { it * 10 }

        assertEquals(DomainResult.Success(20), mapped)
    }

    @Test
    fun givenFailure_whenMapped_thenStaysTheSameFailure() {
        val result: DomainResult<Int, ValidationError> = DomainResult.Failure(testError)

        val mapped = result.map { it * 10 }

        assertEquals(DomainResult.Failure(testError), mapped)
    }

    @Test
    fun givenSuccess_whenFlatMapped_thenChainsTheNextResult() {
        val result: DomainResult<Int, ValidationError> = DomainResult.Success(2)

        val chained = result.flatMap { DomainResult.Success(it + 1) }

        assertEquals(DomainResult.Success(3), chained)
    }

    @Test
    fun givenFailure_whenFlatMapped_thenShortCircuitsWithoutCallingTransform() {
        val result: DomainResult<Int, ValidationError> = DomainResult.Failure(testError)
        var wasCalled = false

        val chained =
            result.flatMap {
                wasCalled = true
                DomainResult.Success(it + 1)
            }

        assertEquals(DomainResult.Failure(testError), chained)
        assertEquals(false, wasCalled)
    }

    @Test
    fun givenSuccess_whenFolded_thenCallsOnSuccess() {
        val result: DomainResult<Int, ValidationError> = DomainResult.Success(2)

        val folded = result.fold(onSuccess = { "value=$it" }, onFailure = { "error" })

        assertEquals("value=2", folded)
    }

    @Test
    fun givenFailure_whenFolded_thenCallsOnFailure() {
        val result: DomainResult<Int, ValidationError> = DomainResult.Failure(testError)

        val folded = result.fold(onSuccess = { "value=$it" }, onFailure = { "error" })

        assertEquals("error", folded)
    }

    @Test
    fun givenSuccess_whenGetOrNull_thenReturnsTheValue() {
        val result: DomainResult<Int, ValidationError> = DomainResult.Success(2)

        assertEquals(2, result.getOrNull())
    }

    @Test
    fun givenFailure_whenGetOrNull_thenReturnsNull() {
        val result: DomainResult<Int, ValidationError> = DomainResult.Failure(testError)

        assertEquals(null, result.getOrNull())
    }

    @Test
    fun givenSuccess_whenErrorOrNull_thenReturnsNull() {
        val result: DomainResult<Int, ValidationError> = DomainResult.Success(2)

        assertEquals(null, result.errorOrNull())
    }

    @Test
    fun givenFailure_whenErrorOrNull_thenReturnsTheError() {
        val result: DomainResult<Int, ValidationError> = DomainResult.Failure(testError)

        assertEquals(testError, result.errorOrNull())
    }
}
