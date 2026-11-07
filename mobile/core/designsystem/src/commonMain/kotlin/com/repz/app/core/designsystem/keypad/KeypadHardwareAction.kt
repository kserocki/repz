package com.repz.app.core.designsystem.keypad

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

/** A sheet-level action a hardware key can trigger, as opposed to a [KeypadInput] edit (plan §3.5). */
internal enum class KeypadHardwareAction { Next, Dismiss }

private val DIGIT_KEYS =
    mapOf(
        Key.Zero to 0,
        Key.NumPad0 to 0,
        Key.One to 1,
        Key.NumPad1 to 1,
        Key.Two to 2,
        Key.NumPad2 to 2,
        Key.Three to 3,
        Key.NumPad3 to 3,
        Key.Four to 4,
        Key.NumPad4 to 4,
        Key.Five to 5,
        Key.NumPad5 to 5,
        Key.Six to 6,
        Key.NumPad6 to 6,
        Key.Seven to 7,
        Key.NumPad7 to 7,
        Key.Eight to 8,
        Key.NumPad8 to 8,
        Key.Nine to 9,
        Key.NumPad9 to 9,
    )

private val DECIMAL_KEYS = setOf(Key.Period, Key.Comma, Key.NumPadDot)

/**
 * Maps a hardware key-down to the [KeypadInput] it represents (plan §3.5): digits, the decimal
 * separator (locale-independent: both `.` and `,` are accepted), Backspace, Delete → Clear, DPad
 * Up/Down → ±. [KeypadRules] already ignores digits/the separator/Backspace in RPE mode, so this
 * mapping doesn't need to know the current mode.
 */
internal fun KeyEvent.toKeypadInputOrNull(): KeypadInput? {
    if (type != KeyEventType.KeyDown) return null
    val digit = DIGIT_KEYS[key]
    return when {
        digit != null -> KeypadInput.Digit(digit)
        key in DECIMAL_KEYS -> KeypadInput.DecimalSeparator
        key == Key.Backspace -> KeypadInput.Backspace
        key == Key.Delete -> KeypadInput.Clear
        key == Key.DirectionUp -> KeypadInput.Increase
        key == Key.DirectionDown -> KeypadInput.Decrease
        else -> null
    }
}

/** Maps a hardware key-down to a sheet-level action: Enter/numpad Enter → Next, Escape → dismiss. */
internal fun KeyEvent.toKeypadHardwareActionOrNull(): KeypadHardwareAction? {
    if (type != KeyEventType.KeyDown) return null
    return when (key) {
        Key.Enter, Key.NumPadEnter -> KeypadHardwareAction.Next
        Key.Escape -> KeypadHardwareAction.Dismiss
        else -> null
    }
}

/**
 * Routes a hardware key-down to [onInput] (an edit, [KeypadInput]) or to [onNext]/[onDismiss] (a
 * sheet-level [KeypadHardwareAction]), per plan §3.5 (K11: digits, the decimal separator,
 * Backspace, Delete → Clear, Enter → Next, Escape → dismiss, DPad Up/Down → ±). Extracted out of
 * `NumberKeypadSheetStateless` (review-1 F5) so that composable's body stays a plain layout
 * assembly.
 */
internal fun Modifier.keypadHardwareKeys(
    onInput: (KeypadInput) -> Boolean,
    onNext: () -> Unit,
    onDismiss: () -> Unit,
): Modifier =
    onPreviewKeyEvent { event ->
        val input = event.toKeypadInputOrNull()
        if (input != null) {
            onInput(input)
            return@onPreviewKeyEvent true
        }
        when (event.toKeypadHardwareActionOrNull()) {
            KeypadHardwareAction.Next -> {
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
    }
