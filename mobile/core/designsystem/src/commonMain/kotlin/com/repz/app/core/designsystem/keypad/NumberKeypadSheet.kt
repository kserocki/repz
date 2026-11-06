package com.repz.app.core.designsystem.keypad

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.repz.app.core.designsystem.format.FixedDecimal
import com.repz.app.core.designsystem.format.RepzNumberSymbols
import com.repz.app.core.designsystem.format.rememberRepzNumberSymbols
import com.repz.app.core.designsystem.generated.resources.Res
import com.repz.app.core.designsystem.generated.resources.keypad_clear
import com.repz.app.core.designsystem.generated.resources.keypad_context_line
import com.repz.app.core.designsystem.generated.resources.keypad_decimal_separator
import com.repz.app.core.designsystem.generated.resources.keypad_decrease_description
import com.repz.app.core.designsystem.generated.resources.keypad_decrease_label
import com.repz.app.core.designsystem.generated.resources.keypad_delete_digit
import com.repz.app.core.designsystem.generated.resources.keypad_empty_value
import com.repz.app.core.designsystem.generated.resources.keypad_hide
import com.repz.app.core.designsystem.generated.resources.keypad_increase_description
import com.repz.app.core.designsystem.generated.resources.keypad_increase_label
import com.repz.app.core.designsystem.generated.resources.keypad_mode_reps
import com.repz.app.core.designsystem.generated.resources.keypad_mode_rpe
import com.repz.app.core.designsystem.generated.resources.keypad_mode_weight
import com.repz.app.core.designsystem.generated.resources.keypad_next_description_next_set
import com.repz.app.core.designsystem.generated.resources.keypad_next_description_reps
import com.repz.app.core.designsystem.generated.resources.keypad_next_description_rpe
import com.repz.app.core.designsystem.generated.resources.keypad_next_done
import com.repz.app.core.designsystem.generated.resources.keypad_next_next_set
import com.repz.app.core.designsystem.generated.resources.keypad_next_reps
import com.repz.app.core.designsystem.generated.resources.keypad_next_rpe
import com.repz.app.core.designsystem.generated.resources.keypad_reps_limit
import com.repz.app.core.designsystem.generated.resources.keypad_value_description
import com.repz.app.core.designsystem.generated.resources.keypad_weight_limit
import com.repz.app.core.designsystem.haptics.RepzHaptics
import com.repz.app.core.designsystem.motion.RepzMotion
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.designsystem.theme.RepzTheme
import org.jetbrains.compose.resources.stringResource

private const val HUNDREDTHS_PER_UNIT = 100L
private val SheetTopCornerRadius = 28.dp
private val SheetShape =
    RoundedCornerShape(topStart = SheetTopCornerRadius, topEnd = SheetTopCornerRadius, bottomStart = 0.dp, bottomEnd = 0.dp)
private val DragHandleWidth = 32.dp
private val DragHandleHeight = 4.dp
private val DragHandleRadius = 2.dp
private val ModeSegmentHeight = 36.dp
private val ModeSegmentTouchTarget = 48.dp
private val ModeSegmentEndRadius = ModeSegmentHeight / 2

private val KeypadBufferSaver: Saver<KeypadBuffer, Any> =
    listSaver(
        save = { listOf(it.text, it.isUntouched) },
        restore = { KeypadBuffer(it[0] as String, it[1] as Boolean) },
    )

/**
 * The non-modal bottom sheet that edits one set's weight, reps or RPE (DESIGN_SYSTEM §9, plan
 * §3.5): mode segmented control, numeric display + ± with a context line (or the limit message
 * after a refused keystroke, U5), the chip row, the 4x4 key grid, hardware keys and MOT-10.
 * [request] `null` hides it; the exit animation keeps rendering the last non-null request (the
 * `AnimatedVisibility` risk noted in the plan), driven only by [visible].
 */
@Composable
fun NumberKeypadSheet(
    request: NumberKeypadRequest?,
    onValueChange: (FixedDecimal?) -> Unit,
    onModeSelect: (KeypadMode) -> Unit,
    onStepSelect: (FixedDecimal) -> Unit,
    onNext: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val heldRequest = rememberLatestNonNull(request)
    AnimatedVisibility(
        visible = request != null,
        enter = slideInVertically(animationSpec = RepzMotion.springs.spatialDefault()) { fullHeight -> fullHeight },
        exit = slideOutVertically(animationSpec = RepzMotion.springs.spatialDefault()) { fullHeight -> fullHeight },
        modifier = modifier,
    ) {
        heldRequest?.let { heldRequestValue ->
            NumberKeypadSheetBody(
                request = heldRequestValue,
                onValueChange = onValueChange,
                onModeSelect = onModeSelect,
                onStepSelect = onStepSelect,
                onNext = onNext,
                onDismiss = onDismiss,
            )
        }
    }
}

@Composable
private fun <T> rememberLatestNonNull(value: T?): T? {
    var latest by remember { mutableStateOf(value) }
    if (value != null) latest = value
    return latest
}

@Suppress("LongMethod")
@Composable
private fun NumberKeypadSheetBody(
    request: NumberKeypadRequest,
    onValueChange: (FixedDecimal?) -> Unit,
    onModeSelect: (KeypadMode) -> Unit,
    onStepSelect: (FixedDecimal) -> Unit,
    onNext: () -> Unit,
    onDismiss: () -> Unit,
) {
    val symbols = rememberRepzNumberSymbols()
    val haptics = RepzHaptics.current

    var buffer by
        rememberSaveable(request.targetKey, request.field.mode, stateSaver = KeypadBufferSaver) {
            mutableStateOf(KeypadRules.bufferFor(request.field.value))
        }
    var showLimitError by rememberSaveable(request.targetKey, request.field.mode) { mutableStateOf(false) }

    // Returns whether the input was accepted (`false` on a refusal), so the ± long-press repeat
    // (KeypadKey.kt) knows to stop instead of hammering Reject.
    fun dispatch(input: KeypadInput): Boolean {
        val previousValue = KeypadRules.valueOf(buffer)
        val edit = KeypadRules.apply(buffer, input, request.field)
        val changed = edit.buffer != buffer
        buffer = edit.buffer
        return when {
            edit.isRejected -> {
                haptics.reject()
                if (request.field.mode != KeypadMode.Rpe) showLimitError = true
                false
            }

            changed -> {
                haptics.tick()
                showLimitError = false
                val newValue = KeypadRules.valueOf(buffer)
                if (newValue != previousValue) onValueChange(newValue)
                true
            }

            // Ignored (e.g. a hardware digit while in RPE mode, plan §3.4): no haptic, no change.
            else -> {
                true
            }
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(request.targetKey, request.field.mode) { focusRequester.requestFocus() }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    val input = event.toKeypadInputOrNull()
                    if (input != null) {
                        dispatch(input)
                        return@onPreviewKeyEvent true
                    }
                    when (event.toKeypadHardwareActionOrNull()) {
                        KeypadHardwareAction.Next -> {
                            haptics.tick()
                            onNext()
                            true
                        }

                        KeypadHardwareAction.Dismiss -> {
                            onDismiss()
                            true
                        }

                        null -> {
                            false
                        }
                    }
                }.clip(SheetShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(horizontal = RepzSpacing.lg, vertical = RepzSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(RepzSpacing.lg),
    ) {
        Box(
            modifier =
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = DragHandleWidth, height = DragHandleHeight)
                    .background(MaterialTheme.colorScheme.outline, RoundedCornerShape(DragHandleRadius)),
        )
        KeypadModeRow(
            modes = request.modes,
            selectedMode = request.field.mode,
            onModeClick = { mode ->
                haptics.tick()
                onModeSelect(mode)
            },
        )
        KeypadDisplayRow(
            request = request,
            buffer = buffer,
            showLimitError = showLimitError,
            symbols = symbols,
            onIncrease = { dispatch(KeypadInput.Increase) },
            onDecrease = { dispatch(KeypadInput.Decrease) },
        )
        KeypadChipRow(
            field = request.field,
            showStepChip = request.stepOptions.isNotEmpty(),
            symbols = symbols,
            onSetValue = { value -> dispatch(KeypadInput.Set(value)) },
            onStepClick = {
                haptics.tick()
                onStepSelect(nextStepOption(request.field.step, request.stepOptions))
            },
        )
        KeypadGrid(
            field = request.field,
            symbols = symbols,
            onDigit = { digit -> dispatch(KeypadInput.Digit(digit)) },
            onDecimal = { dispatch(KeypadInput.DecimalSeparator) },
            onBackspace = { dispatch(KeypadInput.Backspace) },
            onClear = { dispatch(KeypadInput.Clear) },
            onSetRpe = { value -> dispatch(KeypadInput.Set(value)) },
            decimalSeparatorDescription = stringResource(Res.string.keypad_decimal_separator),
            backspaceDescription = stringResource(Res.string.keypad_delete_digit),
            clearLabel = stringResource(Res.string.keypad_clear),
            hideDescription = stringResource(Res.string.keypad_hide),
            onHide = onDismiss,
            nextLabel = request.nextAction.label(),
            nextDescription = request.nextAction.contentDescription(),
            onNext = {
                haptics.tick()
                onNext()
            },
        )
    }
}

private fun nextStepOption(
    current: FixedDecimal,
    options: List<FixedDecimal>,
): FixedDecimal {
    if (options.isEmpty()) return current
    val index = options.indexOf(current)
    return if (index == -1) options.first() else options[(index + 1) % options.size]
}

@Composable
private fun KeypadMode.label(): String =
    when (this) {
        KeypadMode.Weight -> stringResource(Res.string.keypad_mode_weight)
        KeypadMode.Reps -> stringResource(Res.string.keypad_mode_reps)
        KeypadMode.Rpe -> stringResource(Res.string.keypad_mode_rpe)
    }

@Composable
private fun KeypadNextAction.label(): String =
    when (this) {
        KeypadNextAction.Reps -> stringResource(Res.string.keypad_next_reps)
        KeypadNextAction.Rpe -> stringResource(Res.string.keypad_next_rpe)
        KeypadNextAction.NextSet -> stringResource(Res.string.keypad_next_next_set)
        KeypadNextAction.Done -> stringResource(Res.string.keypad_next_done)
    }

@Composable
private fun KeypadNextAction.contentDescription(): String =
    when (this) {
        KeypadNextAction.Reps -> stringResource(Res.string.keypad_next_description_reps)

        KeypadNextAction.Rpe -> stringResource(Res.string.keypad_next_description_rpe)

        KeypadNextAction.NextSet -> stringResource(Res.string.keypad_next_description_next_set)

        // "Done" is already self-descriptive; there is no destination to name (plan U1).
        KeypadNextAction.Done -> stringResource(Res.string.keypad_next_done)
    }

/** The Weight/Reps/[RPE] segmented control (DESIGN_SYSTEM §9); hidden when there is only one mode. */
@Composable
private fun KeypadModeRow(
    modes: List<KeypadMode>,
    selectedMode: KeypadMode,
    onModeClick: (KeypadMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (modes.size <= 1) return

    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(RepzSpacing.xxs)) {
        for ((index, mode) in modes.withIndex()) {
            val isSelected = mode == selectedMode
            val shape =
                RoundedCornerShape(
                    topStart = if (index == 0) ModeSegmentEndRadius else 0.dp,
                    bottomStart = if (index == 0) ModeSegmentEndRadius else 0.dp,
                    topEnd = if (index == modes.lastIndex) ModeSegmentEndRadius else 0.dp,
                    bottomEnd = if (index == modes.lastIndex) ModeSegmentEndRadius else 0.dp,
                )
            val segmentContainerColor =
                if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest
            Box(
                modifier = Modifier.weight(1f).heightIn(min = ModeSegmentTouchTarget),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = ModeSegmentHeight)
                            .clip(shape)
                            .background(segmentContainerColor)
                            .clickable(role = Role.Button, onClick = { onModeClick(mode) })
                            .semantics { selected = isSelected },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = mode.label(),
                        style = MaterialTheme.typography.labelLarge,
                        color =
                            if (isSelected) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                    )
                }
            }
        }
    }
}
