package com.repz.app.core.domain.validation

import com.repz.app.core.domain.model.Weight
import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.domain.result.ValidationError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
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
        assertEquals(1..7, ValidationLimits.WEEKLY_WORKOUT_GOAL_RANGE)
    }

    @Test
    fun givenZeroSecondsRest_whenValidated_thenItPasses() {
        assertEquals(DomainResult.Success(Duration.ZERO), ValidationLimits.validateRest(Duration.ZERO))
    }

    @Test
    fun givenTheMaximumRest_whenValidated_thenItPasses() {
        val max = ValidationLimits.REST_MAX

        assertEquals(DomainResult.Success(max), ValidationLimits.validateRest(max))
    }

    @Test
    fun givenRestOneSecondOverTheMaximum_whenValidated_thenFailsWithRestOutOfRange() {
        val overMax = ValidationLimits.REST_MAX + 1.seconds

        assertEquals(
            DomainResult.Failure(ValidationError.RestOutOfRange(ValidationLimits.REST_MAX)),
            ValidationLimits.validateRest(overMax),
        )
    }

    @Test
    fun givenANegativeRest_whenValidated_thenFailsWithRestOutOfRange() {
        assertEquals(
            DomainResult.Failure(ValidationError.RestOutOfRange(ValidationLimits.REST_MAX)),
            ValidationLimits.validateRest((-1).seconds),
        )
    }

    @Test
    fun givenRestWithASubSecondComponent_whenValidated_thenFailsWithRestOutOfRange() {
        assertEquals(
            DomainResult.Failure(ValidationError.RestOutOfRange(ValidationLimits.REST_MAX)),
            ValidationLimits.validateRest(90.5.seconds),
        )
    }

    @Test
    fun givenTheMinimumWeeklyWorkoutGoal_whenValidated_thenItPasses() {
        assertEquals(DomainResult.Success(1), ValidationLimits.validateWeeklyWorkoutGoal(1))
    }

    @Test
    fun givenTheMaximumWeeklyWorkoutGoal_whenValidated_thenItPasses() {
        assertEquals(DomainResult.Success(7), ValidationLimits.validateWeeklyWorkoutGoal(7))
    }

    @Test
    fun givenAWeeklyWorkoutGoalOfZero_whenValidated_thenFailsWithWeeklyWorkoutGoalOutOfRange() {
        assertEquals(
            DomainResult.Failure(ValidationError.WeeklyWorkoutGoalOutOfRange(ValidationLimits.WEEKLY_WORKOUT_GOAL_RANGE)),
            ValidationLimits.validateWeeklyWorkoutGoal(0),
        )
    }

    @Test
    fun givenAWeeklyWorkoutGoalOfEight_whenValidated_thenFailsWithWeeklyWorkoutGoalOutOfRange() {
        assertEquals(
            DomainResult.Failure(ValidationError.WeeklyWorkoutGoalOutOfRange(ValidationLimits.WEEKLY_WORKOUT_GOAL_RANGE)),
            ValidationLimits.validateWeeklyWorkoutGoal(8),
        )
    }
}
