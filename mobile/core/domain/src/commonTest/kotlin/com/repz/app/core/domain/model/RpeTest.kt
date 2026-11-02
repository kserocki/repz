package com.repz.app.core.domain.model

import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.domain.result.ValidationError
import kotlin.test.Test
import kotlin.test.assertEquals

class RpeTest {
    @Test
    fun givenAllNineValues_whenValidated_thenEachIsAccepted() {
        Rpe.ALL.forEach { rpe -> assertEquals(DomainResult.Success(rpe), Rpe.of(rpe.value)) }
    }

    @Test
    fun givenAValueBelowTheLowestStep_whenValidated_thenFailsWithInvalidRpe() {
        assertEquals(DomainResult.Failure(ValidationError.InvalidRpe), Rpe.of(5.5))
    }

    @Test
    fun givenAValueAboveTheHighestStep_whenValidated_thenFailsWithInvalidRpe() {
        assertEquals(DomainResult.Failure(ValidationError.InvalidRpe), Rpe.of(10.5))
    }

    @Test
    fun givenAValueOffTheHalfStep_whenValidated_thenFailsWithInvalidRpe() {
        assertEquals(DomainResult.Failure(ValidationError.InvalidRpe), Rpe.of(6.25))
    }

    @Test
    fun givenNaN_whenValidated_thenFailsWithInvalidRpe() {
        assertEquals(DomainResult.Failure(ValidationError.InvalidRpe), Rpe.of(Double.NaN))
    }

    @Test
    fun givenInfinity_whenValidated_thenFailsWithInvalidRpe() {
        assertEquals(DomainResult.Failure(ValidationError.InvalidRpe), Rpe.of(Double.POSITIVE_INFINITY))
    }

    @Test
    fun givenAll_whenInspected_thenItHasNineAscendingValues() {
        assertEquals(9, Rpe.ALL.size)
        assertEquals(Rpe.ALL.sortedBy { it.value }, Rpe.ALL)
    }
}
