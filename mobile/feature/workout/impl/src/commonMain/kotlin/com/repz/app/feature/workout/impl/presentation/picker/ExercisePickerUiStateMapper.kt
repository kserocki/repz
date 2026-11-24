package com.repz.app.feature.workout.impl.presentation.picker

import com.repz.app.core.domain.exercise.searchedBy
import com.repz.app.core.domain.model.Equipment
import com.repz.app.core.domain.model.Exercise
import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.model.Muscle
import com.repz.app.core.domain.model.localizedName
import com.repz.app.feature.workout.impl.domain.ExercisePickerData
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/** Maps the reactive [ExercisePickerData] and the in-memory [LocalState] to [ExercisePickerUiState]. */
internal class ExercisePickerUiStateMapper(
    private val languageTag: () -> String,
    private val timeZone: () -> TimeZone,
    private val clock: Clock,
) {
    fun toUiState(
        data: ExercisePickerData?,
        local: LocalState,
    ): ExercisePickerUiState {
        val muscleChips = Muscle.entries.map { MuscleChipUi(it, it in local.filter.muscles) }.toImmutableList()
        val equipmentChips = Equipment.entries.map { EquipmentChipUi(it, it in local.filter.equipment) }.toImmutableList()

        if (data == null) {
            return ExercisePickerUiState(
                isLoading = true,
                mode = local.mode,
                query = local.query,
                muscleChips = muscleChips,
                equipmentChips = equipmentChips,
                activeFilterCount = local.filter.activeCount,
                isSubmitting = local.isSubmitting,
            )
        }

        val tag = languageTag()
        val selectedIds = local.selection.toSet()
        val filtered = data.exercises.filter(local.filter::matches).searchedBy(local.query, tag)
        val results = filtered.map { exercise -> toPickerUi(exercise, tag, selectedIds, local.mode, lastUsedDaysAgo = null) }

        return ExercisePickerUiState(
            isLoading = false,
            mode = local.mode,
            query = local.query,
            muscleChips = muscleChips,
            equipmentChips = equipmentChips,
            activeFilterCount = local.filter.activeCount,
            recent = buildRecent(data, local, tag, selectedIds).toImmutableList(),
            results = results.toImmutableList(),
            resultsHeader = resultsHeader(local, filtered.size),
            emptyState = emptyState(data.exercises, filtered, local),
            selectedCount = if (local.mode == ExercisePickerMode.Add) local.selection.size else 0,
            isSubmitting = local.isSubmitting,
            replaceConfirmation = local.pendingReplace?.confirmation,
        )
    }

    private fun buildRecent(
        data: ExercisePickerData,
        local: LocalState,
        languageTag: String,
        selectedIds: Set<ExerciseId>,
    ): List<PickerExerciseUi> {
        if (local.query.isNotBlank()) return emptyList()

        val exercisesById = data.exercises.associateBy { it.id }
        val today = clock.now().toLocalDateTime(timeZone()).date
        return data.recent
            .mapNotNull { use -> exercisesById[use.exerciseId]?.let { exercise -> exercise to use.lastUsedAt } }
            .filter { (exercise, _) -> local.filter.matches(exercise) }
            .sortedWith(compareByDescending<Pair<Exercise, Instant>> { it.second }.thenBy { it.first.localizedName(languageTag) })
            .map { (exercise, lastUsedAt) ->
                val daysAgo =
                    lastUsedAt
                        .toLocalDateTime(timeZone())
                        .date
                        .daysUntil(today)
                        .coerceAtLeast(0)
                toPickerUi(exercise, languageTag, selectedIds, local.mode, daysAgo)
            }
    }

    private fun toPickerUi(
        exercise: Exercise,
        languageTag: String,
        selectedIds: Set<ExerciseId>,
        mode: ExercisePickerMode,
        lastUsedDaysAgo: Int?,
    ): PickerExerciseUi =
        PickerExerciseUi(
            id = exercise.id,
            name = exercise.localizedName(languageTag),
            primaryMuscle = exercise.primaryMuscle,
            equipment = exercise.equipment,
            isSelected = mode == ExercisePickerMode.Add && exercise.id in selectedIds,
            lastUsedDaysAgo = lastUsedDaysAgo,
        )
}

private fun resultsHeader(
    local: LocalState,
    resultCount: Int,
): PickerResultsHeader =
    if (local.query.isBlank() && !local.filter.isActive) {
        PickerResultsHeader.AllExercises
    } else {
        PickerResultsHeader.Results(resultCount)
    }

private fun emptyState(
    allExercises: List<Exercise>,
    filtered: List<Exercise>,
    local: LocalState,
): PickerEmptyState? =
    when {
        allExercises.isEmpty() -> PickerEmptyState.NoExercises
        filtered.isNotEmpty() -> null
        local.query.isNotBlank() -> PickerEmptyState.NoMatchForQuery(local.query.trim(), local.filter.activeCount)
        else -> PickerEmptyState.NoMatchForFilters(local.filter.activeCount)
    }
