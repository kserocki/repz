package com.repz.app.core.data.exercise

import com.repz.app.core.database.exercise.RecentExerciseUseRow
import com.repz.app.core.domain.exercise.RecentExerciseUse
import com.repz.app.core.domain.model.ExerciseId

internal fun RecentExerciseUseRow.toDomain(): RecentExerciseUse = RecentExerciseUse(ExerciseId(exerciseId), lastUsedAt)

internal fun List<RecentExerciseUseRow>.toDomainList(): List<RecentExerciseUse> = map { it.toDomain() }
