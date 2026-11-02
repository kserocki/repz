package com.repz.app.core.domain.validation

import com.repz.app.core.domain.model.Weight
import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.domain.result.ValidationError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class ValidationLimitsTest {
    @Test
    fun givenANameWithSurroundingSpaces_whenValidated_thenTheTrimmedNameWins() {
        assertEquals(DomainResult.Success("Push day"), ValidationLimits.validateName("  Push day  "))
    }

    @Test
    fun givenABlankName_whenValidated_thenFailsWithNameBlank() {
        assertEquals(DomainResult.Failure(ValidationError.NameBlank), ValidationLimits.validateName("   "))
    }

    @Test
    fun givenAHundredCharacterName_whenValidated_thenItPasses() {
        val name = "a".repeat(ValidationLimits.NAME_MAX_LENGTH)

        assertEquals(DomainResult.Success(name), ValidationLimits.validateName(name))
    }

    @Test
    fun givenAHundredAndOneCharacterName_whenValidated_thenFailsWithNameTooLong() {
        val name = "a".repeat(ValidationLimits.NAME_MAX_LENGTH + 1)

        assertEquals(
            DomainResult.Failure(ValidationError.NameTooLong(ValidationLimits.NAME_MAX_LENGTH)),
            ValidationLimits.validateName(name),
        )
    }

    @Test
    fun givenAHundredCharacterNameWithSurroundingSpaces_whenValidated_thenItPasses() {
        val name = "a".repeat(ValidationLimits.NAME_MAX_LENGTH)

        assertEquals(DomainResult.Success(name), ValidationLimits.validateName("  $name  "))
    }

    @Test
    fun givenNotesAtTheLimit_whenValidated_thenItPasses() {
        val notes = "a".repeat(ValidationLimits.NOTES_MAX_LENGTH)

        assertEquals(DomainResult.Success(notes), ValidationLimits.validateNotes(notes))
    }

    @Test
    fun givenNotesOverTheLimit_whenValidated_thenFailsWithNotesTooLong() {
        val notes = "a".repeat(ValidationLimits.NOTES_MAX_LENGTH + 1)

        assertEquals(
            DomainResult.Failure(ValidationError.NotesTooLong(ValidationLimits.NOTES_MAX_LENGTH)),
            ValidationLimits.validateNotes(notes),
        )
    }

    @Test
    fun givenTheMaximumSetWeight_whenValidated_thenItPasses() {
        val max = ValidationLimits.SET_WEIGHT_MAX

        assertEquals(DomainResult.Success(max), ValidationLimits.validateSetWeight(max))
    }

    @Test
    fun givenOneGramOverTheMaximumSetWeight_whenValidated_thenFailsWithWeightTooHeavy() {
        val overweight = Weight(ValidationLimits.SET_WEIGHT_MAX.grams + 1)

        assertEquals(
            DomainResult.Failure(ValidationError.WeightTooHeavy(ValidationLimits.SET_WEIGHT_MAX)),
            ValidationLimits.validateSetWeight(overweight),
        )
    }

    @Test
    fun givenZeroReps_whenValidated_thenItPasses() {
        assertEquals(DomainResult.Success(0), ValidationLimits.validateReps(0))
    }

    @Test
    fun givenTheMaximumReps_whenValidated_thenItPasses() {
        assertEquals(DomainResult.Success(999), ValidationLimits.validateReps(999))
    }

    @Test
    fun givenNegativeReps_whenValidated_thenFailsWithRepsOutOfRange() {
        assertEquals(
            DomainResult.Failure(ValidationError.RepsOutOfRange(ValidationLimits.REPS_RANGE)),
            ValidationLimits.validateReps(-1),
        )
    }

    @Test
    fun givenRepsOverTheMaximum_whenValidated_thenFailsWithRepsOutOfRange() {
        assertEquals(
            DomainResult.Failure(ValidationError.RepsOutOfRange(ValidationLimits.REPS_RANGE)),
            ValidationLimits.validateReps(1000),
        )
    }

    @Test
    fun givenTheConstants_whenInspected_thenTheyMatchTheServerLimits() {
        assertEquals(100, ValidationLimits.NAME_MAX_LENGTH)
        assertEquals(2000, ValidationLimits.NOTES_MAX_LENGTH)
        assertEquals(50, ValidationLimits.EXERCISES_PER_WORKOUT_MAX)
        assertEquals(50, ValidationLimits.SETS_PER_EXERCISE_MAX)
        assertEquals(50, ValidationLimits.EXERCISES_PER_TEMPLATE_MAX)
        assertEquals(1..20, ValidationLimits.TARGET_SETS_RANGE)
        assertEquals(0..999, ValidationLimits.REPS_RANGE)
        assertEquals(3600.seconds, ValidationLimits.REST_MAX)
        assertEquals(Weight(1_000_000), ValidationLimits.SET_WEIGHT_MAX)
    }
}
