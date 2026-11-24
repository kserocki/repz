package com.repz.app.feature.workout.impl.presentation.picker

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.component.rememberRepzSnackbarHostState
import com.repz.app.core.designsystem.theme.RepzTheme
import com.repz.app.core.domain.model.Equipment
import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.model.Muscle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.uuid.Uuid

private const val PREVIEW_UUID_SEED_DIGITS = 12

/** Shared by every `ExercisePicker*Previews.kt` file (all in this package, `internal` so no import is
 * needed); `ExercisePickerScreen.kt` keeps its own single preview for Konsist N4. */
@Composable
internal fun ExercisePickerPreview(
    darkTheme: Boolean,
    state: ExercisePickerUiState,
) {
    val snackbarHostState = rememberRepzSnackbarHostState()
    RepzTheme(darkTheme = darkTheme) {
        ExercisePickerContent(state = state, onIntent = {}, snackbarHostState = snackbarHostState)
    }
}

internal fun previewPickerUuid(seed: Int): Uuid =
    Uuid.parse("00000000-0000-0000-0000-" + seed.toString().padStart(PREVIEW_UUID_SEED_DIGITS, '0'))

internal fun previewPickerExercise(
    seed: Int,
    name: String,
    muscle: Muscle,
    equipment: Equipment,
    isSelected: Boolean = false,
    lastUsedDaysAgo: Int? = null,
): PickerExerciseUi =
    PickerExerciseUi(
        id = ExerciseId(previewPickerUuid(seed)),
        name = name,
        primaryMuscle = muscle,
        equipment = equipment,
        isSelected = isSelected,
        lastUsedDaysAgo = lastUsedDaysAgo,
    )

internal fun previewMuscleChips(selected: Set<Muscle> = emptySet()): ImmutableList<MuscleChipUi> =
    Muscle.entries.map { muscle -> MuscleChipUi(muscle, muscle in selected) }.toImmutableList()

internal fun previewEquipmentChips(selected: Set<Equipment> = emptySet()): ImmutableList<EquipmentChipUi> =
    Equipment.entries.map { equipment -> EquipmentChipUi(equipment, equipment in selected) }.toImmutableList()

internal val PreviewBenchPress = previewPickerExercise(seed = 1, name = "Bench Press", muscle = Muscle.CHEST, equipment = Equipment.BARBELL)
internal val PreviewOverheadPress =
    previewPickerExercise(seed = 2, name = "Overhead Press", muscle = Muscle.SHOULDERS, equipment = Equipment.BARBELL)
internal val PreviewLegPress =
    previewPickerExercise(seed = 3, name = "Leg Press", muscle = Muscle.QUADRICEPS, equipment = Equipment.MACHINE)
internal val PreviewBarbellRow = previewPickerExercise(seed = 4, name = "Barbell Row", muscle = Muscle.BACK, equipment = Equipment.BARBELL)
internal val PreviewBicepCurl = previewPickerExercise(seed = 5, name = "Bicep Curl", muscle = Muscle.BICEPS, equipment = Equipment.DUMBBELL)

internal val ExercisePickerLoadingState =
    ExercisePickerUiState(isLoading = true, muscleChips = previewMuscleChips(), equipmentChips = previewEquipmentChips())

internal val ExercisePickerAddDefaultState =
    ExercisePickerUiState(
        isLoading = false,
        muscleChips = previewMuscleChips(),
        equipmentChips = previewEquipmentChips(),
        recent =
            persistentListOf(
                PreviewBenchPress.copy(lastUsedDaysAgo = 0),
                PreviewOverheadPress.copy(lastUsedDaysAgo = 1),
            ),
        results = persistentListOf(PreviewBenchPress, PreviewBarbellRow, PreviewBicepCurl, PreviewLegPress, PreviewOverheadPress),
        resultsHeader = PickerResultsHeader.AllExercises,
    )

internal val ExercisePickerSearchingState =
    ExercisePickerUiState(
        isLoading = false,
        query = "press",
        muscleChips = previewMuscleChips(),
        equipmentChips = previewEquipmentChips(),
        results = persistentListOf(PreviewBenchPress, PreviewOverheadPress, PreviewLegPress),
        resultsHeader = PickerResultsHeader.Results(count = 3),
    )

internal val ExercisePickerSelectedState =
    ExercisePickerUiState(
        isLoading = false,
        muscleChips = previewMuscleChips(),
        equipmentChips = previewEquipmentChips(),
        recent = persistentListOf(PreviewBenchPress.copy(isSelected = true, lastUsedDaysAgo = 0)),
        results =
            persistentListOf(
                PreviewBarbellRow.copy(isSelected = true),
                PreviewBicepCurl.copy(isSelected = true),
                PreviewLegPress,
                PreviewOverheadPress,
            ),
        resultsHeader = PickerResultsHeader.AllExercises,
        selectedCount = 3,
    )

internal val ExercisePickerNoMatchQueryState =
    ExercisePickerUiState(
        isLoading = false,
        query = "zzz",
        muscleChips = previewMuscleChips(selected = setOf(Muscle.CHEST)),
        equipmentChips = previewEquipmentChips(selected = setOf(Equipment.BARBELL)),
        activeFilterCount = 2,
        resultsHeader = PickerResultsHeader.Results(count = 0),
        emptyState = PickerEmptyState.NoMatchForQuery(query = "zzz", activeFilterCount = 2),
    )

internal val ExercisePickerNoMatchFiltersState =
    ExercisePickerUiState(
        isLoading = false,
        muscleChips = previewMuscleChips(selected = setOf(Muscle.CALVES)),
        equipmentChips = previewEquipmentChips(selected = setOf(Equipment.CABLE)),
        activeFilterCount = 2,
        resultsHeader = PickerResultsHeader.Results(count = 0),
        emptyState = PickerEmptyState.NoMatchForFilters(activeFilterCount = 2),
    )

internal val ExercisePickerNoExercisesState =
    ExercisePickerUiState(
        isLoading = false,
        muscleChips = previewMuscleChips(),
        equipmentChips = previewEquipmentChips(),
        emptyState = PickerEmptyState.NoExercises,
    )

internal val ExercisePickerReplaceState =
    ExercisePickerUiState(
        isLoading = false,
        mode = ExercisePickerMode.Replace,
        muscleChips = previewMuscleChips(),
        equipmentChips = previewEquipmentChips(),
        recent = persistentListOf(PreviewBenchPress.copy(lastUsedDaysAgo = 0)),
        results = persistentListOf(PreviewBarbellRow, PreviewBicepCurl, PreviewLegPress, PreviewOverheadPress),
        resultsHeader = PickerResultsHeader.AllExercises,
    )

internal val ExercisePickerReplaceDialogState =
    ExercisePickerReplaceState.copy(
        replaceConfirmation = ReplaceConfirmationUi(replacedExerciseName = "Bench Press", completedSetCount = 2),
    )

@Preview
@Composable
private fun ExercisePickerLoadingLightPreview() = ExercisePickerPreview(darkTheme = false, state = ExercisePickerLoadingState)

@Preview
@Composable
private fun ExercisePickerLoadingDarkPreview() = ExercisePickerPreview(darkTheme = true, state = ExercisePickerLoadingState)

@Preview
@Composable
private fun ExercisePickerAddDefaultDarkPreview() = ExercisePickerPreview(darkTheme = true, state = ExercisePickerAddDefaultState)
