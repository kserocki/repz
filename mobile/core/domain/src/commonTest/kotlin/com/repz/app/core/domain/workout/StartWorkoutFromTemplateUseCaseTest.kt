package com.repz.app.core.domain.workout

import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.model.Weight
import com.repz.app.core.domain.model.WorkoutTemplateId
import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.testing.id.SequentialIdGenerator
import com.repz.app.core.testing.template.FakeWorkoutTemplateRepository
import com.repz.app.core.testing.time.MutableClock
import com.repz.app.core.testing.workout.FakeWorkoutRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class StartWorkoutFromTemplateUseCaseTest {
    private fun useCase(
        workouts: FakeWorkoutRepository,
        templates: FakeWorkoutTemplateRepository,
    ): StartWorkoutFromTemplateUseCase =
        StartWorkoutFromTemplateUseCase(
            workouts = workouts,
            templates = templates,
            lastPerformance = FindLastPerformanceUseCase(workouts),
            clock = MutableClock(Instant.fromEpochSeconds(0)),
            idGenerator = SequentialIdGenerator(),
        )

    @Test
    fun givenAnUnknownTemplate_whenStarted_thenItFails() =
        runTest {
            val useCase = useCase(FakeWorkoutRepository(), FakeWorkoutTemplateRepository())

            val result = useCase(WorkoutTemplateId(testUuid(1)))

            assertEquals(DomainResult.Failure(StartWorkoutError.TemplateNotFound), result)
        }

    @Test
    fun givenAnActiveWorkoutAlreadyExists_whenStarted_thenItFails() =
        runTest {
            val existingActive = testActiveWorkout(seed = 1)
            val workouts = FakeWorkoutRepository(listOf(existingActive))
            val template = testWorkoutTemplate(seed = 1)
            val templates = FakeWorkoutTemplateRepository(listOf(template))

            val result = useCase(workouts, templates)(template.id)

            assertEquals(DomainResult.Failure(StartWorkoutError.WorkoutAlreadyActive(existingActive.id)), result)
        }

    @Test
    fun givenATemplateWithNoHistory_whenStarted_thenItCreatesAWorkoutWithTheTemplatesNameAndPrefilledSets() =
        runTest {
            val template =
                testWorkoutTemplate(
                    seed = 1,
                    name = "Push A",
                    exercises = listOf(testTemplateExercise(seed = 1, targetSets = 2, targetReps = 6..8)),
                )
            val workouts = FakeWorkoutRepository()
            val templates = FakeWorkoutTemplateRepository(listOf(template))

            val result = useCase(workouts, templates)(template.id) as DomainResult.Success

            val saved = workouts.findWorkoutIncludingDeleted(result.value)
            assertEquals("Push A", saved?.name)
            assertEquals(template.id, saved?.templateId)
            val exercise = saved?.exercises?.single()
            assertEquals(2, exercise?.sets?.size)
            exercise?.sets?.forEach { set ->
                assertEquals(Weight.ZERO, set.weight)
                assertEquals(6, set.reps)
                assertNull(set.completedAt)
            }
        }

    @Test
    fun givenATemplateWithHistory_whenStarted_thenEachSetTakesThatPositionsLastPerformance() =
        runTest {
            val exerciseId = ExerciseId(testUuid(1))
            val template =
                testWorkoutTemplate(
                    seed = 1,
                    exercises = listOf(testTemplateExercise(seed = 1, exerciseId = exerciseId, targetSets = 1, targetReps = 6..8)),
                )
            val lastPerformanceSet = testWorkoutSet(seed = 2, weight = Weight.fromKilograms(70.0), reps = 7)
            val pastWorkout =
                testFinishedWorkout(
                    seed = 2,
                    exercises = listOf(testWorkoutExercise(seed = 2, exerciseId = exerciseId, sets = listOf(lastPerformanceSet))),
                )
            val workouts = FakeWorkoutRepository(listOf(pastWorkout))
            val templates = FakeWorkoutTemplateRepository(listOf(template))

            val result = useCase(workouts, templates)(template.id) as DomainResult.Success

            val saved = workouts.findWorkoutIncludingDeleted(result.value)
            val set =
                saved
                    ?.exercises
                    ?.single()
                    ?.sets
                    ?.single()
            assertEquals(Weight.fromKilograms(70.0), set?.weight)
            assertEquals(7, set?.reps)
        }
}
