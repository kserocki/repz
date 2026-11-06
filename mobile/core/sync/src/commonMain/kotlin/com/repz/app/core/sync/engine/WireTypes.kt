package com.repz.app.core.sync.engine

import com.repz.app.core.network.dto.SyncTypeDto
import com.repz.app.core.network.dto.sync.PushChangeDto
import com.repz.app.core.network.dto.sync.SyncChangeDto

internal fun PushChangeDto.wireType(): SyncTypeDto =
    when (this) {
        is PushChangeDto.UpsertWorkout -> SyncTypeDto.WORKOUT
        is PushChangeDto.UpsertTemplate -> SyncTypeDto.TEMPLATE
        is PushChangeDto.UpsertCustomExercise -> SyncTypeDto.EXERCISE
        is PushChangeDto.Delete -> type
    }

internal fun SyncChangeDto.wireType(): SyncTypeDto =
    when (this) {
        is SyncChangeDto.Workout -> SyncTypeDto.WORKOUT
        is SyncChangeDto.Template -> SyncTypeDto.TEMPLATE
        is SyncChangeDto.Exercise -> SyncTypeDto.EXERCISE
        is SyncChangeDto.Tombstone -> type
    }
