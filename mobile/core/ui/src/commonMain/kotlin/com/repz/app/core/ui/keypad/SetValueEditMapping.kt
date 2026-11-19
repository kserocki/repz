package com.repz.app.core.ui.keypad

import com.repz.app.core.designsystem.format.FixedDecimal
import com.repz.app.core.designsystem.keypad.KeypadMode
import com.repz.app.core.domain.model.Rpe
import com.repz.app.core.domain.model.UnitSystem
import com.repz.app.core.domain.preferences.WeightStep
import com.repz.app.core.domain.result.getOrNull
import com.repz.app.core.ui.format.WeightFormat

private const val HUNDREDTHS_PER_UNIT = 100L
private const val HUNDREDTHS_PER_UNIT_DOUBLE = 100.0

/** The result of one accepted edit of the field currently shown; `null` clears it. */
internal fun SetValueField.toSetValueEdit(
    amount: FixedDecimal?,
    unit: UnitSystem,
): SetValueEdit =
    when (this) {
        SetValueField.WEIGHT -> SetValueEdit.Weight(amount?.let { WeightFormat.fromDisplay(it, unit) })
        SetValueField.REPS -> SetValueEdit.Reps(amount?.let { (it.hundredths / HUNDREDTHS_PER_UNIT).toInt() })
        SetValueField.RPE -> SetValueEdit.Rpe(amount?.let { it.toRpe() })
    }

/** The sheet only ever offers [Rpe.ALL] as RPE choices, so this conversion can never fail. */
private fun FixedDecimal.toRpe(): Rpe =
    Rpe.of(hundredths / HUNDREDTHS_PER_UNIT_DOUBLE).getOrNull()
        ?: error("Keypad RPE choices are always one of Rpe.ALL: $hundredths hundredths is not a valid RPE")

/** The mode the sheet just selected, as a [SetValueField]. */
internal fun KeypadMode.toSetValueField(): SetValueField =
    when (this) {
        KeypadMode.Weight -> SetValueField.WEIGHT
        KeypadMode.Reps -> SetValueField.REPS
        KeypadMode.Rpe -> SetValueField.RPE
    }

/** The step the sheet's Step chip just cycled to, as a [WeightStep]; it is always one of [WeightStep.optionsFor]. */
internal fun FixedDecimal.toWeightStep(unit: UnitSystem): WeightStep = WeightStep.optionsFor(unit).first { it.toFixedDecimal() == this }

internal fun WeightStep.toFixedDecimal(): FixedDecimal = FixedDecimal((amountInUnit * HUNDREDTHS_PER_UNIT_DOUBLE).toLong())

internal fun Int.toFixedDecimal(): FixedDecimal = FixedDecimal.ofWhole(toLong())

internal fun Rpe.toFixedDecimal(): FixedDecimal = FixedDecimal((value * HUNDREDTHS_PER_UNIT_DOUBLE).toLong())
