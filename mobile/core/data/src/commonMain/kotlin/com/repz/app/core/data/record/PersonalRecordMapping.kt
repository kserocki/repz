package com.repz.app.core.data.record

import co.touchlab.kermit.Logger
import com.repz.app.core.database.record.PersonalRecordEntity
import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.model.Weight
import com.repz.app.core.domain.model.WorkoutId
import com.repz.app.core.domain.model.WorkoutSetId
import com.repz.app.core.domain.record.PersonalRecord
import com.repz.app.core.domain.record.PersonalRecordType

/** An unknown stored `type` (R5) is logged and dropped: `null` here, filtered out of a list by the caller. */
internal fun PersonalRecordEntity.toDomainOrNull(logger: Logger): PersonalRecord? =
    type.toPersonalRecordTypeOrNull(logger)?.let { recordType ->
        PersonalRecord(
            exerciseId = ExerciseId(exerciseId),
            type = recordType,
            weight = Weight(weightGrams),
            reps = reps,
            workoutId = WorkoutId(workoutId),
            setId = WorkoutSetId(setId),
            achievedAt = achievedAt,
        )
    }

internal fun List<PersonalRecordEntity>.toDomainList(logger: Logger): List<PersonalRecord> = mapNotNull { it.toDomainOrNull(logger) }

private fun String.toPersonalRecordTypeOrNull(logger: Logger): PersonalRecordType? =
    when (this) {
        "est_1rm" -> {
            PersonalRecordType.EST_1RM
        }

        "heaviest" -> {
            PersonalRecordType.HEAVIEST
        }

        "best_set_volume" -> {
            PersonalRecordType.BEST_SET_VOLUME
        }

        "most_reps" -> {
            PersonalRecordType.MOST_REPS
        }

        else -> {
            logger.e { "Unknown personal_records.type value: $this" }
            null
        }
    }

private fun PersonalRecordType.toWireText(): String =
    when (this) {
        PersonalRecordType.EST_1RM -> "est_1rm"
        PersonalRecordType.HEAVIEST -> "heaviest"
        PersonalRecordType.BEST_SET_VOLUME -> "best_set_volume"
        PersonalRecordType.MOST_REPS -> "most_reps"
    }

internal fun PersonalRecord.toEntity(): PersonalRecordEntity =
    PersonalRecordEntity(
        exerciseId = exerciseId.value,
        type = type.toWireText(),
        weightGrams = weight.grams,
        reps = reps,
        workoutId = workoutId.value,
        setId = setId.value,
        achievedAt = achievedAt,
    )
