package com.repz.app.core.ui.keypad

import androidx.compose.runtime.Immutable
import com.repz.app.core.designsystem.keypad.KeypadNextAction
import com.repz.app.core.domain.model.Rpe
import com.repz.app.core.domain.model.UnitSystem
import com.repz.app.core.domain.model.Weight
import com.repz.app.core.domain.preferences.WeightStep

/** The three fields a workout set's keypad can edit. */
enum class SetValueField { WEIGHT, REPS, RPE }

/** A set's values, in domain types; `null` means empty. */
@Immutable
data class SetValues(
    val weight: Weight?,
    val reps: Int?,
    val rpe: Rpe?,
)

/**
 * What the caller (a workout ViewModel) asks [SetValueKeypad] to render: the field currently being
 * edited plus everything needed to build its [com.repz.app.core.designsystem.keypad.KeypadField],
 * a frozen contract for the workout feature.
 */
@Immutable
data class SetValueKeypadRequest(
    /** Stable per set, e.g. `setId.toString()`; the field is added internally. */
    val targetKey: String,
    val field: SetValueField,
    /** The set's values now. */
    val current: SetValues,
    /** "Same as last" source, chosen by the caller; `null` hides the chip. */
    val last: SetValues?,
    val unit: UnitSystem,
    val weightStep: WeightStep,
    /** `false` hides the RPE mode. */
    val isRpeEnabled: Boolean,
    /** Localized by the caller, e.g. "Bench Press · Set 3". */
    val contextLabel: String,
    /** What Next does for this field, decided by the caller's flow. */
    val nextAction: KeypadNextAction,
)

/** One accepted change, emitted live as the user types. */
sealed interface SetValueEdit {
    data class Weight(
        val weight: com.repz.app.core.domain.model.Weight?,
    ) : SetValueEdit

    data class Reps(
        val reps: Int?,
    ) : SetValueEdit

    data class Rpe(
        val rpe: com.repz.app.core.domain.model.Rpe?,
    ) : SetValueEdit
}
