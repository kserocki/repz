package com.repz.app.core.domain.record

import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.model.Weight
import com.repz.app.core.domain.model.WorkoutId
import com.repz.app.core.domain.model.WorkoutSetId
import kotlin.time.Instant

enum class PersonalRecordType { EST_1RM, HEAVIEST, BEST_SET_VOLUME, MOST_REPS }

/**
 * Carries the record-holding set itself, not only its value, so the UI can show "Heaviest × 8"
 * (the set's own reps) and link straight to it via [setId].
 */
data class PersonalRecord(
    val exerciseId: ExerciseId,
    val type: PersonalRecordType,
    val weight: Weight,
    val reps: Int,
    val workoutId: WorkoutId,
    val setId: WorkoutSetId,
    val achievedAt: Instant,
) {
    val value: PersonalRecordValue
        get() =
            when (type) {
                PersonalRecordType.MOST_REPS -> PersonalRecordValue.Reps(reps)

                PersonalRecordType.EST_1RM,
                PersonalRecordType.HEAVIEST,
                PersonalRecordType.BEST_SET_VOLUME,
                -> PersonalRecordValue.Load(weight)
            }
}

sealed interface PersonalRecordValue {
    data class Load(
        val weight: Weight,
    ) : PersonalRecordValue

    data class Reps(
        val count: Int,
    ) : PersonalRecordValue
}
