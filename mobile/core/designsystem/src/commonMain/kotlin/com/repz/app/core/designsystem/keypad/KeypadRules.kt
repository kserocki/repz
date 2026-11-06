package com.repz.app.core.designsystem.keypad

import com.repz.app.core.designsystem.format.FixedDecimal
import com.repz.app.core.designsystem.format.RepzNumberSymbols

private const val HUNDREDTHS_PER_UNIT = 100L
private const val LEADING_ZERO = "0"

/**
 * The pure editing rules behind `NumberKeypadSheet` (plan §3.4, no Compose): untouched-replace /
 * touched-append typing, the U5 "refuse past the limit" rule, and ± stepping without snapping
 * (ADR-0022). [apply] never throws; an input that can't be accepted comes back with the buffer
 * unchanged and `isRejected` set — except a key that doesn't apply to the field's mode at all
 * (digits/separator/backspace in RPE mode), which is silently ignored (`isRejected = false`): it
 * can only be reached through a hardware keyboard, since the sheet hides those keys in RPE mode.
 */
object KeypadRules {
    /** An untouched buffer showing [value] (or empty for `null`), e.g. just opened or retargeted. */
    fun bufferFor(value: FixedDecimal?): KeypadBuffer =
        KeypadBuffer(text = value?.format(RepzNumberSymbols.Default) ?: "", isUntouched = true)

    fun apply(
        buffer: KeypadBuffer,
        input: KeypadInput,
        field: KeypadField,
    ): KeypadEdit =
        when (input) {
            is KeypadInput.Digit -> applyDigit(buffer, input.digit, field)
            KeypadInput.DecimalSeparator -> applyDecimalSeparator(buffer, field)
            KeypadInput.Backspace -> applyBackspace(buffer, field)
            KeypadInput.Clear -> KeypadEdit(KeypadBuffer("", isUntouched = true), isRejected = false)
            KeypadInput.Increase -> applyStep(buffer, field, increase = true)
            KeypadInput.Decrease -> applyStep(buffer, field, increase = false)
            is KeypadInput.Set -> KeypadEdit(bufferFor(input.value), isRejected = false)
        }

    /** `"82."` -> 82.00, `""` -> `null`. A missing fraction digit is treated as `0`. */
    fun valueOf(buffer: KeypadBuffer): FixedDecimal? {
        val text = buffer.text
        if (text.isEmpty()) return null

        val separatorIndex = text.indexOf('.')
        return if (separatorIndex == -1) {
            FixedDecimal.ofWhole(text.toLong())
        } else {
            val whole = text.substring(0, separatorIndex).ifEmpty { LEADING_ZERO }.toLong()
            val fraction =
                text
                    .substring(separatorIndex + 1)
                    .padEnd(2, '0')
                    .take(2)
                    .toLong()
            FixedDecimal(whole * HUNDREDTHS_PER_UNIT + fraction)
        }
    }

    private fun applyDigit(
        buffer: KeypadBuffer,
        digit: Int,
        field: KeypadField,
    ): KeypadEdit {
        if (field.mode == KeypadMode.Rpe) return KeypadEdit(buffer, isRejected = false)
        val base = if (buffer.isUntouched) "" else buffer.text
        val appended = appendDigit(base, digit, field.fractionDigits)
        return if (appended == null) KeypadEdit(buffer, isRejected = true) else acceptIfWithinMax(appended, field, buffer)
    }

    private fun applyDecimalSeparator(
        buffer: KeypadBuffer,
        field: KeypadField,
    ): KeypadEdit {
        if (field.mode == KeypadMode.Rpe) return KeypadEdit(buffer, isRejected = false)
        val base = if (buffer.isUntouched) "" else buffer.text
        val canType = field.fractionDigits > 0 && !base.contains('.')
        return if (!canType) {
            KeypadEdit(buffer, isRejected = true)
        } else {
            acceptIfWithinMax(if (base.isEmpty()) "$LEADING_ZERO." else "$base.", field, buffer)
        }
    }

    private fun applyBackspace(
        buffer: KeypadBuffer,
        field: KeypadField,
    ): KeypadEdit {
        if (field.mode == KeypadMode.Rpe) return KeypadEdit(buffer, isRejected = false)
        return if (buffer.text.isEmpty()) {
            KeypadEdit(buffer, isRejected = true)
        } else {
            KeypadEdit(KeypadBuffer(buffer.text.dropLast(1), isUntouched = false), isRejected = false)
        }
    }

    private fun applyStep(
        buffer: KeypadBuffer,
        field: KeypadField,
        increase: Boolean,
    ): KeypadEdit {
        val current = valueOf(buffer)
        val stepped = if (increase) stepIncrease(current, field) else stepDecrease(current, field)
        return if (stepped == null) KeypadEdit(buffer, isRejected = true) else KeypadEdit(bufferFor(stepped), isRejected = false)
    }

    /** Clamped to [KeypadField.max]; refused only when [current] is already there. From empty: `min + step` (RPE: `min`). */
    private fun stepIncrease(
        current: FixedDecimal?,
        field: KeypadField,
    ): FixedDecimal? {
        if (current == null) return if (field.mode == KeypadMode.Rpe) field.min else field.min + field.step
        return if (current >= field.max) {
            null
        } else {
            val nextHundredths = current.hundredths + field.step.hundredths
            if (nextHundredths > field.max.hundredths) field.max else FixedDecimal(nextHundredths)
        }
    }

    /** Refused if the result would go below [KeypadField.min], or the buffer is empty. Never snaps (ADR-0022). */
    private fun stepDecrease(
        current: FixedDecimal?,
        field: KeypadField,
    ): FixedDecimal? {
        if (current == null) return null
        val nextHundredths = current.hundredths - field.step.hundredths
        return if (nextHundredths < field.min.hundredths) null else FixedDecimal(nextHundredths)
    }
}

/** `null` means the digit can't be appended (the fraction-digit limit was reached). */
private fun appendDigit(
    text: String,
    digit: Int,
    fractionDigits: Int,
): String? {
    val separatorIndex = text.indexOf('.')
    return if (separatorIndex != -1) {
        val fractionLength = text.length - separatorIndex - 1
        if (fractionLength >= fractionDigits) null else text + digit
    } else {
        if (text == LEADING_ZERO) digit.toString() else text + digit
    }
}

private fun acceptIfWithinMax(
    text: String,
    field: KeypadField,
    previous: KeypadBuffer,
): KeypadEdit {
    val candidate = KeypadBuffer(text, isUntouched = false)
    val value = KeypadRules.valueOf(candidate)
    return if (value != null && value > field.max) KeypadEdit(previous, isRejected = true) else KeypadEdit(candidate, isRejected = false)
}
