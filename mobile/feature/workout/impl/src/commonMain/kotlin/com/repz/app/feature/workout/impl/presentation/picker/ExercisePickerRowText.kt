package com.repz.app.feature.workout.impl.presentation.picker

import androidx.compose.runtime.Composable
import com.repz.app.core.ui.label.label
import com.repz.app.feature.workout.impl.generated.resources.Res
import com.repz.app.feature.workout.impl.generated.resources.picker_exercise_meta
import com.repz.app.feature.workout.impl.generated.resources.picker_exercise_meta_recent
import com.repz.app.feature.workout.impl.generated.resources.picker_last_used_days_ago
import com.repz.app.feature.workout.impl.generated.resources.picker_last_used_today
import com.repz.app.feature.workout.impl.generated.resources.picker_last_used_yesterday
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** A picker row's supporting text (DESIGN_SYSTEM §9 picker composition): "<muscle> · <equipment>",
 * with a third "<last used>" segment on a `recent` row ([PickerExerciseUi.lastUsedDaysAgo] non-`null`). */
@Composable
internal fun PickerExerciseUi.supportingText(): String {
    val muscleLabel = primaryMuscle.label()
    val equipmentLabel = equipment.label()
    val daysAgo = lastUsedDaysAgo
    return if (daysAgo != null) {
        stringResource(Res.string.picker_exercise_meta_recent, muscleLabel, equipmentLabel, lastUsedText(daysAgo))
    } else {
        stringResource(Res.string.picker_exercise_meta, muscleLabel, equipmentLabel)
    }
}

@Composable
private fun lastUsedText(daysAgo: Int): String =
    when (daysAgo) {
        0 -> stringResource(Res.string.picker_last_used_today)
        1 -> stringResource(Res.string.picker_last_used_yesterday)
        else -> pluralStringResource(Res.plurals.picker_last_used_days_ago, daysAgo, daysAgo)
    }
