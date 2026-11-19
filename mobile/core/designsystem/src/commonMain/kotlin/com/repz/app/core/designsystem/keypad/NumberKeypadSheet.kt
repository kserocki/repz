package com.repz.app.core.designsystem.keypad

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.repz.app.core.designsystem.format.FixedDecimal
import com.repz.app.core.designsystem.format.rememberRepzNumberSymbols
import com.repz.app.core.designsystem.generated.resources.Res
import com.repz.app.core.designsystem.generated.resources.keypad_clear
import com.repz.app.core.designsystem.generated.resources.keypad_decimal_separator
import com.repz.app.core.designsystem.generated.resources.keypad_delete_digit
import com.repz.app.core.designsystem.generated.resources.keypad_hide
import com.repz.app.core.designsystem.generated.resources.keypad_next_description_next_set
import com.repz.app.core.designsystem.generated.resources.keypad_next_description_reps
import com.repz.app.core.designsystem.generated.resources.keypad_next_description_rpe
import com.repz.app.core.designsystem.generated.resources.keypad_next_done
import com.repz.app.core.designsystem.generated.resources.keypad_next_next_set
import com.repz.app.core.designsystem.generated.resources.keypad_next_reps
import com.repz.app.core.designsystem.generated.resources.keypad_next_rpe
import com.repz.app.core.designsystem.haptics.RepzHaptics
import com.repz.app.core.designsystem.motion.RepzMotion
import com.repz.app.core.designsystem.theme.RepzSpacing
import org.jetbrains.compose.resources.stringResource

private val SheetTopCornerRadius = 28.dp
private val SheetShape =
    RoundedCornerShape(topStart = SheetTopCornerRadius, topEnd = SheetTopCornerRadius, bottomStart = 0.dp, bottomEnd = 0.dp)
private val DragHandleWidth = 32.dp
private val DragHandleHeight = 4.dp
private val DragHandleRadius = 2.dp

private val KeypadBufferSaver: Saver<KeypadBuffer, Any> =
    listSaver(
        save = { listOf(it.text, it.isUntouched) },
        restore = { KeypadBuffer(it[0] as String, it[1] as Boolean) },
    )

internal val RetargetKeySaver: Saver<LatestValueHolder<String>, String> =
    Saver(save = { it.value }, restore = { LatestValueHolder(it) })

/**
 * The non-modal bottom sheet that edits one set's weight, reps or RPE (DESIGN_SYSTEM §9): mode
 * segmented control, numeric display + ± with a context line (or the limit message after a
 * refused keystroke), the chip row, the 4x4 key grid, hardware keys and MOT-10.
 * [request] `null` hides it; the exit animation keeps rendering the last non-null request while
 * it plays out, driven only by whether [request] is `null`.
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
            NumberKeypadSheetStateful(
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

/** A plain (non-state) holder for the latest non-null value seen so far: [rememberLatestNonNull]
 * only needs a fresh value within the same composition pass that updates it, so a `mutableStateOf`
 * isn't needed here. Internal (not private) so [RetargetKeySaver] is directly unit-testable. */
internal class LatestValueHolder<T>(
    var value: T,
)

@Composable
private fun <T> rememberLatestNonNull(value: T?): T? {
    val holder = remember { LatestValueHolder(value) }
    if (value != null) holder.value = value
    return holder.value
}

/** Identifies which field is being edited, combining the target and the mode: switching either
 * means a different value is now being typed. [NumberKeypadSheetStateful] resets its buffer
 * whenever this key changes. Internal (not private) so it's directly unit-testable. */
internal fun retargetKeyOf(request: NumberKeypadRequest): String = "${request.targetKey}\u0000${request.field.mode.name}"

/** Everything [NumberKeypadSheetStateful] needs from its buffer/dispatch machinery, handed back as
 * plain values so that composable's own body stays a short wiring assembly. */
private class KeypadDispatchState(
    val buffer: KeypadBuffer,
    val showLimitError: Boolean,
    val dispatch: (KeypadInput) -> Boolean,
)

/**
 * Builds the edit buffer and [KeypadDispatchState.dispatch] with one stable identity for the
 * lifetime of the call site: retargeting resets the buffer's value in place instead of recreating
 * its `MutableState`, and `dispatch` reads [request], [onValueChange] and the current haptics
 * through [rememberUpdatedState], so it never closes over a stale instance (ADR-0028).
 */
@Composable
private fun rememberKeypadDispatchState(
    request: NumberKeypadRequest,
    onValueChange: (FixedDecimal?) -> Unit,
): KeypadDispatchState {
    val currentHaptics = rememberUpdatedState(RepzHaptics.current)
    val currentRequest = rememberUpdatedState(request)
    val currentOnValueChange = rememberUpdatedState(onValueChange)

    var buffer by rememberSaveable(stateSaver = KeypadBufferSaver) {
        mutableStateOf(KeypadRules.bufferFor(request.field.value))
    }
    var showLimitError by rememberSaveable { mutableStateOf(false) }

    // A plain (non-state) holder, not a MutableState: retargetKey is pure bookkeeping, so
    // comparing and updating it here doesn't read-then-write a snapshot state the same
    // composition depends on. It's still restored via rememberSaveable, alongside buffer and
    // showLimitError, so a configuration change or process death can't strand it on the request
    // it started with: without that, a caller restoring a different request (a different target
    // or mode) would go undetected and the old buffer would be edited as if it were the new one.
    val retargetKey = rememberSaveable(saver = RetargetKeySaver) { LatestValueHolder(retargetKeyOf(request)) }
    val currentRetargetKey = retargetKeyOf(request)
    if (currentRetargetKey != retargetKey.value) {
        retargetKey.value = currentRetargetKey
        buffer = KeypadRules.bufferFor(request.field.value)
        showLimitError = false
    }

    // Returns whether the input was accepted (`false` on a refusal), so the ± long-press repeat
    // (KeypadKey.kt) knows to stop instead of hammering Reject. Remembered once (no keys): its own
    // reference never changes, so no downstream skip can ever leave a button bound to a stale copy.
    val dispatch: (KeypadInput) -> Boolean =
        remember {
            { input ->
                val field = currentRequest.value.field
                val previousValue = KeypadRules.valueOf(buffer)
                val edit = KeypadRules.apply(buffer, input, field)
                val changed = edit.buffer != buffer
                buffer = edit.buffer
                when (edit.rejection) {
                    // Only an out-of-range refusal shows the limit message (DESIGN_SYSTEM §13).
                    // A malformed keystroke (an extra fraction digit, a second separator, backspace
                    // on empty) still rejects and fires the same haptic, but was never out of range.
                    KeypadRejection.OutOfRange -> {
                        currentHaptics.value.reject()
                        showLimitError = true
                        false
                    }

                    KeypadRejection.Format -> {
                        currentHaptics.value.reject()
                        false
                    }

                    null -> {
                        if (changed) {
                            currentHaptics.value.tick()
                            showLimitError = false
                            val newValue = KeypadRules.valueOf(buffer)
                            if (newValue != previousValue) currentOnValueChange.value(newValue)
                            true
                        } else {
                            // Ignored (e.g. a hardware digit in RPE mode): no haptic, no change.
                            true
                        }
                    }
                }
            }
        }

    return KeypadDispatchState(buffer, showLimitError, dispatch)
}

/**
 * Owns the sheet's only state (the edit buffer and the limit-error flag, built by
 * [rememberKeypadDispatchState]) and all haptics. Delegates layout to the stateless
 * [NumberKeypadSheetStateless], so that composable (and its previews) can render any buffer/error
 * combination directly, without going through this public entry point's own state.
 */
@Composable
private fun NumberKeypadSheetStateful(
    request: NumberKeypadRequest,
    onValueChange: (FixedDecimal?) -> Unit,
    onModeSelect: (KeypadMode) -> Unit,
    onStepSelect: (FixedDecimal) -> Unit,
    onNext: () -> Unit,
    onDismiss: () -> Unit,
) {
    val haptics = RepzHaptics.current
    val dispatchState = rememberKeypadDispatchState(request, onValueChange)

    NumberKeypadSheetStateless(
        request = request,
        buffer = dispatchState.buffer,
        showLimitError = dispatchState.showLimitError,
        onInput = dispatchState.dispatch,
        onModeSelect = { mode ->
            haptics.tick()
            onModeSelect(mode)
        },
        onStepSelect = {
            haptics.tick()
            onStepSelect(nextStepOption(request.field.step, request.stepOptions))
        },
        onNext = {
            haptics.tick()
            onNext()
        },
        onDismiss = onDismiss,
    )
}

/**
 * Pure layout: every visual state is a function of [request], [buffer] and [showLimitError], never
 * of its own `remember`ed state, so previews can render any buffer/error combination directly by
 * passing them in. [onInput] is the stateful parent's `dispatch`. Provides `onSurface` as
 * [LocalContentColor] once here, so every child text/icon reads correctly in dark mode by default,
 * without a `Surface`.
 */
@Composable
internal fun NumberKeypadSheetStateless(
    request: NumberKeypadRequest,
    buffer: KeypadBuffer,
    showLimitError: Boolean,
    onInput: (KeypadInput) -> Boolean,
    onModeSelect: (KeypadMode) -> Unit,
    onStepSelect: () -> Unit,
    onNext: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val symbols = rememberRepzNumberSymbols()
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(request.targetKey, request.field.mode) { focusRequester.requestFocus() }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .focusable()
                .keypadHardwareKeys(onInput = onInput, onNext = onNext, onDismiss = onDismiss)
                .clip(SheetShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(horizontal = RepzSpacing.lg, vertical = RepzSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(RepzSpacing.lg),
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            KeypadDragHandle(modifier = Modifier.align(Alignment.CenterHorizontally))
            KeypadModeRow(
                modes = request.modes,
                selectedMode = request.field.mode,
                onModeClick = onModeSelect,
            )
            KeypadDisplayRow(
                request = request,
                buffer = buffer,
                showLimitError = showLimitError,
                symbols = symbols,
                onIncrease = { onInput(KeypadInput.Increase) },
                onDecrease = { onInput(KeypadInput.Decrease) },
            )
            KeypadChipRow(
                field = request.field,
                showStepChip = request.stepOptions.isNotEmpty(),
                symbols = symbols,
                onSetValue = { value -> onInput(KeypadInput.Set(value)) },
                onStepClick = onStepSelect,
            )
            KeypadGrid(
                field = request.field,
                symbols = symbols,
                onDigit = { digit -> onInput(KeypadInput.Digit(digit)) },
                onDecimal = { onInput(KeypadInput.DecimalSeparator) },
                onBackspace = { onInput(KeypadInput.Backspace) },
                onClear = { onInput(KeypadInput.Clear) },
                onSetRpe = { value -> onInput(KeypadInput.Set(value)) },
                decimalSeparatorDescription = stringResource(Res.string.keypad_decimal_separator),
                backspaceDescription = stringResource(Res.string.keypad_delete_digit),
                clearLabel = stringResource(Res.string.keypad_clear),
                hideDescription = stringResource(Res.string.keypad_hide),
                onHide = onDismiss,
                nextLabel = request.nextAction.label(),
                nextDescription = request.nextAction.contentDescription(),
                onNext = onNext,
            )
        }
    }
}

/** The 32×4 `outline` drag handle (visual only; the sheet has no drag-to-dismiss yet). */
@Composable
private fun KeypadDragHandle(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .size(width = DragHandleWidth, height = DragHandleHeight)
                .background(MaterialTheme.colorScheme.outline, RoundedCornerShape(DragHandleRadius)),
    )
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

        // "Done" is already self-descriptive; there is no destination to name.
        KeypadNextAction.Done -> stringResource(Res.string.keypad_next_done)
    }
