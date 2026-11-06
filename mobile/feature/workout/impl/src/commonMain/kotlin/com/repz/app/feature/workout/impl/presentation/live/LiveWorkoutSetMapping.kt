package com.repz.app.feature.workout.impl.presentation.live

import androidx.compose.runtime.Composable
import com.repz.app.core.designsystem.component.workout.RepzAccessibilityAction
import com.repz.app.core.designsystem.component.workout.RepzMenuItem
import com.repz.app.core.designsystem.component.workout.RepzSetField
import com.repz.app.core.designsystem.component.workout.RepzSetType
import com.repz.app.core.domain.model.SetType
import com.repz.app.core.domain.model.UnitSystem
import com.repz.app.core.domain.model.Weight
import com.repz.app.core.ui.format.formatSetWeight
import com.repz.app.core.ui.format.formatSetWeightWithUnit
import com.repz.app.feature.workout.impl.generated.resources.Res
import com.repz.app.feature.workout.impl.generated.resources.set_action_delete
import com.repz.app.feature.workout.impl.generated.resources.set_action_mark_drop
import com.repz.app.feature.workout.impl.generated.resources.set_action_mark_failure
import com.repz.app.feature.workout.impl.generated.resources.set_action_mark_normal
import com.repz.app.feature.workout.impl.generated.resources.set_action_mark_warmup
import com.repz.app.feature.workout.impl.generated.resources.set_badge_drop
import com.repz.app.feature.workout.impl.generated.resources.set_badge_failure
import com.repz.app.feature.workout.impl.generated.resources.set_badge_warmup
import com.repz.app.feature.workout.impl.generated.resources.set_complete
import com.repz.app.feature.workout.impl.generated.resources.set_last_line
import com.repz.app.feature.workout.impl.generated.resources.set_previous_value
import com.repz.app.feature.workout.impl.generated.resources.set_type_drop
import com.repz.app.feature.workout.impl.generated.resources.set_type_failure
import com.repz.app.feature.workout.impl.generated.resources.set_type_normal
import com.repz.app.feature.workout.impl.generated.resources.set_type_warmup
import com.repz.app.feature.workout.impl.generated.resources.set_uncomplete
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** The badge letter/number shown and read out for a set (Q3, board 05): numbers count only Normal
 * sets, letters identify Warm-up/Drop/Failure. Shared by [LiveSetUi] and the keypad's own target,
 * which carries the same [SetType]/`normalSetNumber` pair without a full [LiveSetUi]. */
@Composable
internal fun badgeLabel(
    type: SetType,
    normalSetNumber: Int?,
): String =
    when (type) {
        SetType.WARMUP -> stringResource(Res.string.set_badge_warmup)
        SetType.DROP -> stringResource(Res.string.set_badge_drop)
        SetType.FAILURE -> stringResource(Res.string.set_badge_failure)
        SetType.NORMAL -> normalSetNumber?.toString().orEmpty()
    }

@Composable
internal fun LiveSetUi.badgeLabel(): String = badgeLabel(type, normalSetNumber)

internal fun SetType.toRepzSetType(): RepzSetType =
    when (this) {
        SetType.WARMUP -> RepzSetType.Warmup
        SetType.NORMAL -> RepzSetType.Normal
        SetType.DROP -> RepzSetType.Drop
        SetType.FAILURE -> RepzSetType.Failure
    }

/** `null` if there's no editing target or it's RPE (off while RPE mode is disabled, T12). */
internal fun SetField?.toRepzSetField(): RepzSetField? =
    when (this) {
        SetField.Weight -> RepzSetField.Weight
        SetField.Reps -> RepzSetField.Reps
        SetField.Rpe, null -> null
    }

@Composable
internal fun previousText(
    previous: PreviousSetUi?,
    unit: UnitSystem,
): String? = previous?.let { stringResource(Res.string.set_previous_value, formatSetWeight(it.weight, unit), it.reps) }

@Composable
internal fun lastLineText(
    previous: PreviousSetUi?,
    unit: UnitSystem,
): String? = previous?.let { stringResource(Res.string.set_last_line, formatSetWeight(it.weight, unit), it.reps) }

/** The complete/uncomplete check's content description (§12 example): describes what tapping it
 * will do next, not the set's current state. */
@Composable
internal fun completeContentDescription(
    set: LiveSetUi,
    badgeLabel: String,
    unit: UnitSystem,
): String =
    if (set.isCompleted) {
        stringResource(Res.string.set_uncomplete, badgeLabel)
    } else {
        val reps = set.reps ?: 0
        val weightWithUnit = formatSetWeightWithUnit(set.weight ?: Weight.ZERO, unit)
        pluralStringResource(Res.plurals.set_complete, reps, badgeLabel, weightWithUnit, reps)
    }

/** The badge menu's four rows (Q3-A): tapping one calls [onTypeSelect]. Only `*Screen`/`*Content`
 * composables may know [LiveWorkoutIntent] (AGENTS.md §4, Konsist N8), so this and every other
 * helper here take plain values and present-tense callbacks instead. */
@Composable
internal fun typeMenuItems(
    currentType: SetType,
    onTypeSelect: (SetType) -> Unit,
): ImmutableList<RepzMenuItem> =
    persistentListOf(
        RepzMenuItem(
            label = stringResource(Res.string.set_type_warmup),
            onClick = { onTypeSelect(SetType.WARMUP) },
            isChecked = currentType == SetType.WARMUP,
        ),
        RepzMenuItem(
            label = stringResource(Res.string.set_type_normal),
            onClick = { onTypeSelect(SetType.NORMAL) },
            isChecked = currentType == SetType.NORMAL,
        ),
        RepzMenuItem(
            label = stringResource(Res.string.set_type_drop),
            onClick = { onTypeSelect(SetType.DROP) },
            isChecked = currentType == SetType.DROP,
        ),
        RepzMenuItem(
            label = stringResource(Res.string.set_type_failure),
            onClick = { onTypeSelect(SetType.FAILURE) },
            isChecked = currentType == SetType.FAILURE,
        ),
    )

/** "Delete set" plus "Mark as …" for every type other than [currentType] (§12, board 18). */
@Composable
internal fun setAccessibilityActions(
    currentType: SetType,
    onDelete: () -> Unit,
    onTypeSelect: (SetType) -> Unit,
): ImmutableList<RepzAccessibilityAction> {
    val deleteLabel = stringResource(Res.string.set_action_delete)
    val warmupLabel = stringResource(Res.string.set_action_mark_warmup)
    val dropLabel = stringResource(Res.string.set_action_mark_drop)
    val failureLabel = stringResource(Res.string.set_action_mark_failure)
    val normalLabel = stringResource(Res.string.set_action_mark_normal)

    val actions = mutableListOf(RepzAccessibilityAction(deleteLabel, onDelete))
    if (currentType != SetType.WARMUP) actions += RepzAccessibilityAction(warmupLabel) { onTypeSelect(SetType.WARMUP) }
    if (currentType != SetType.NORMAL) actions += RepzAccessibilityAction(normalLabel) { onTypeSelect(SetType.NORMAL) }
    if (currentType != SetType.DROP) actions += RepzAccessibilityAction(dropLabel) { onTypeSelect(SetType.DROP) }
    if (currentType != SetType.FAILURE) actions += RepzAccessibilityAction(failureLabel) { onTypeSelect(SetType.FAILURE) }
    return actions.toImmutableList()
}
