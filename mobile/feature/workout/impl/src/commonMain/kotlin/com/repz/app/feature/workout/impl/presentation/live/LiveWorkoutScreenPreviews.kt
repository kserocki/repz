package com.repz.app.feature.workout.impl.presentation.live

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.component.rememberRepzSnackbarHostState
import com.repz.app.core.designsystem.theme.RepzTheme
import com.repz.app.core.domain.model.Muscle
import com.repz.app.core.domain.model.SetType
import com.repz.app.core.domain.model.UnitSystem
import com.repz.app.core.domain.model.Weight
import com.repz.app.core.domain.model.WorkoutExerciseId
import com.repz.app.core.domain.model.WorkoutSetId
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

private const val PREVIEW_UUID_SEED_DIGITS = 12

/** Shared by this file and `LiveWorkoutScreenPreviews2.kt` (both in this package, `internal` so no
 * import is needed); `LiveWorkoutScreen.kt` keeps its own single preview for Konsist N4. */
@Composable
internal fun LiveWorkoutPreview(
    darkTheme: Boolean,
    state: LiveWorkoutUiState,
) {
    val snackbarHostState = rememberRepzSnackbarHostState()
    RepzTheme(darkTheme = darkTheme) {
        LiveWorkoutContent(state = state, onIntent = {}, snackbarHostState = snackbarHostState)
    }
}

internal fun previewUuid(seed: Int): Uuid = Uuid.parse("00000000-0000-0000-0000-" + seed.toString().padStart(PREVIEW_UUID_SEED_DIGITS, '0'))

internal fun previewSet(
    seed: Int,
    type: SetType = SetType.NORMAL,
    normalSetNumber: Int? = null,
    weightKg: Double? = null,
    reps: Int? = null,
    previousWeightKg: Double? = null,
    previousReps: Int? = null,
    isPrefilled: Boolean = false,
    isCompleted: Boolean = false,
): LiveSetUi =
    LiveSetUi(
        id = WorkoutSetId(previewUuid(seed)),
        type = type,
        normalSetNumber = normalSetNumber,
        weight = weightKg?.let { Weight.fromKilograms(it) },
        reps = reps,
        previous =
            if (previousWeightKg != null && previousReps != null) {
                PreviousSetUi(Weight.fromKilograms(previousWeightKg), previousReps)
            } else {
                null
            },
        isPrefilled = isPrefilled,
        isCompleted = isCompleted,
    )

internal fun previewExercise(
    seed: Int,
    name: String?,
    muscle: Muscle?,
    sets: List<LiveSetUi>,
    notes: String = "",
    isEditingNotes: Boolean = false,
    canAddSet: Boolean = true,
): LiveExerciseUi =
    LiveExerciseUi(
        id = WorkoutExerciseId(previewUuid(seed)),
        name = name,
        primaryMuscle = muscle,
        notes = notes,
        isEditingNotes = isEditingNotes,
        completedSetCount = sets.count { it.isCompleted },
        canAddSet = canAddSet,
        sets = sets.toImmutableList(),
    )

// Bench Press: warm-up + 3 normal sets covering empty/prefilled/completed (board 05).
internal val BenchPressSets =
    listOf(
        previewSet(
            seed = 1,
            type = SetType.WARMUP,
            previousWeightKg = 60.0,
            previousReps = 10,
            weightKg = 60.0,
            reps = 10,
            isCompleted = true,
        ),
        previewSet(seed = 2, normalSetNumber = 1, previousWeightKg = 80.0, previousReps = 8, weightKg = 80.0, reps = 8, isCompleted = true),
        previewSet(seed = 3, normalSetNumber = 2, previousWeightKg = 80.0, previousReps = 8, weightKg = 82.5, reps = 8, isCompleted = true),
        previewSet(seed = 4, normalSetNumber = 3, previousWeightKg = 80.0, previousReps = 7, weightKg = 82.5, reps = 8, isPrefilled = true),
    )
internal val BenchPress = previewExercise(seed = 10, name = "Bench Press", muscle = Muscle.CHEST, sets = BenchPressSets)

// Overhead Press: empty normal set + a drop set, no history (board 09's "add exercise" prefill).
private val OverheadPressSets =
    listOf(
        previewSet(seed = 5, normalSetNumber = 1),
        previewSet(seed = 6, type = SetType.DROP, weightKg = 40.0, reps = 12),
        previewSet(
            seed = 7,
            type = SetType.FAILURE,
            previousWeightKg = 70.0,
            previousReps = 9,
            weightKg = 70.0,
            reps = 9,
            isCompleted = true,
        ),
    )
private val OverheadPress = previewExercise(seed = 11, name = "Overhead Press", muscle = Muscle.SHOULDERS, sets = OverheadPressSets)

internal val LiveWorkoutLoadingState = LiveWorkoutUiState(isLoading = true)

internal val LiveWorkoutEmptyState =
    LiveWorkoutUiState(isLoading = false, workoutName = "Wednesday workout", elapsed = 12.seconds, canFinish = false)

internal val LiveWorkoutInProgressState =
    LiveWorkoutUiState(
        isLoading = false,
        workoutName = "Push A",
        elapsed = 34.minutes + 12.seconds,
        unit = UnitSystem.KG,
        exercises = persistentListOf(BenchPress, OverheadPress),
        canFinish = true,
    )

@Preview
@Composable
private fun LiveWorkoutLoadingLightPreview() = LiveWorkoutPreview(darkTheme = false, state = LiveWorkoutLoadingState)

@Preview
@Composable
private fun LiveWorkoutLoadingDarkPreview() = LiveWorkoutPreview(darkTheme = true, state = LiveWorkoutLoadingState)

@Preview
@Composable
private fun LiveWorkoutEmptyLightPreview() = LiveWorkoutPreview(darkTheme = false, state = LiveWorkoutEmptyState)

@Preview
@Composable
private fun LiveWorkoutEmptyDarkPreview() = LiveWorkoutPreview(darkTheme = true, state = LiveWorkoutEmptyState)

@Preview(locale = "pl")
@Composable
private fun LiveWorkoutEmptyPolishPreview() = LiveWorkoutPreview(darkTheme = false, state = LiveWorkoutEmptyState)

@Preview
@Composable
private fun LiveWorkoutInProgressLightPreview() = LiveWorkoutPreview(darkTheme = false, state = LiveWorkoutInProgressState)

@Preview
@Composable
private fun LiveWorkoutInProgressDarkPreview() = LiveWorkoutPreview(darkTheme = true, state = LiveWorkoutInProgressState)
