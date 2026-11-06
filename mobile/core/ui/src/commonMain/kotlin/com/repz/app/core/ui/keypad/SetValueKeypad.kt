package com.repz.app.core.ui.keypad

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.repz.app.core.designsystem.keypad.NumberKeypadSheet
import com.repz.app.core.domain.preferences.WeightStep
import com.repz.app.core.ui.format.weightUnitLabel

/**
 * The workout feature's keypad entry point (plan §8.1): resolves the unit label and maps
 * [request] to [com.repz.app.core.designsystem.keypad.NumberKeypadRequest], then renders
 * [NumberKeypadSheet]. The caller never sees
 * [com.repz.app.core.designsystem.format.FixedDecimal] or the sheet itself.
 */
@Composable
fun SetValueKeypad(
    request: SetValueKeypadRequest?,
    onValueChange: (SetValueEdit) -> Unit,
    onFieldSelect: (SetValueField) -> Unit,
    onWeightStepChange: (WeightStep) -> Unit,
    onNext: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val numberKeypadRequest = request?.let { it.toNumberKeypadRequest(weightUnitLabel(it.unit)) }

    NumberKeypadSheet(
        request = numberKeypadRequest,
        onValueChange = { amount ->
            val field = request?.field ?: return@NumberKeypadSheet
            onValueChange(field.toSetValueEdit(amount, request.unit))
        },
        onModeSelect = { mode -> onFieldSelect(mode.toSetValueField()) },
        onStepSelect = { step ->
            val unit = request?.unit ?: return@NumberKeypadSheet
            onWeightStepChange(step.toWeightStep(unit))
        },
        onNext = onNext,
        onDismiss = onDismiss,
        modifier = modifier,
    )
}
