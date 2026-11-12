package com.repz.app.core.designsystem.keypad

import androidx.compose.runtime.Immutable
import com.repz.app.core.designsystem.format.FixedDecimal

/** The three fields `NumberKeypadSheet` can edit (plan §3.4). */
enum class KeypadMode { Weight, Reps, Rpe }

/** What the Next key does for the field currently shown, decided by the caller. */
enum class KeypadNextAction { Reps, Rpe, NextSet, Done }

/**
 * What one mode edits, in display units (plan §3.4). `core:ui`'s `SetValueKeypadMapping` builds
 * this from domain values; the sheet never sees `Weight`/`Rpe`.
 */
@Immutable
data class KeypadField(
    val mode: KeypadMode,
    /** `null` = empty. */
    val value: FixedDecimal?,
    /** Weight/reps 0, RPE 6.0. */
    val min: FixedDecimal,
    /** 1000 kg / 2204.62 lb / 999 reps / 10.0 RPE. */
    val max: FixedDecimal,
    /** Weight: the per-unit preference. Reps: 1. RPE: 0.5. */
    val step: FixedDecimal,
    /** Weight 2, reps 0, RPE 1 (RPE is never typed). */
    val fractionDigits: Int,
    /** `null` hides the "Same as last" chip. */
    val sameAsLast: FixedDecimal?,
    /** Chips after "Same as last"; empty in RPE mode. */
    val quickValues: List<FixedDecimal>,
    /** RPE mode only: the 9 keys ([com.repz.app.core.domain.model.Rpe.ALL] in display units); empty otherwise. */
    val choices: List<FixedDecimal>,
)

/** Everything `NumberKeypadSheet` needs to render one mode of one target (plan §3.4). */
@Immutable
data class NumberKeypadRequest(
    /** A new key resets the edit buffer. */
    val targetKey: String,
    val field: KeypadField,
    /** `[Weight, Reps]` or `[Weight, Reps, Rpe]`. */
    val modes: List<KeypadMode>,
    /** "kg" / "lb", localized by the caller. */
    val weightUnitLabel: String,
    /** E.g. "Bench Press · Set 3", localized by the caller. */
    val contextLabel: String,
    val nextAction: KeypadNextAction,
    /** Empty hides the Step chip; weight mode only. */
    val stepOptions: List<FixedDecimal>,
)

/** The edit buffer `KeypadRules` operates on. `text` uses `.` internally regardless of locale; `""` is empty. */
@Immutable
data class KeypadBuffer(
    val text: String,
    val isUntouched: Boolean,
)

/** One key press or chip tap handed to `KeypadRules.apply`. */
sealed interface KeypadInput {
    data class Digit(
        val digit: Int,
    ) : KeypadInput

    data object DecimalSeparator : KeypadInput

    data object Backspace : KeypadInput

    data object Clear : KeypadInput

    data object Increase : KeypadInput

    data object Decrease : KeypadInput

    /** A chip, "Same as last" or an RPE key: replaces the buffer outright and leaves it untouched. */
    data class Set(
        val value: FixedDecimal,
    ) : KeypadInput
}

/**
 * Why [KeypadRules.apply] refused an input (DESIGN_SYSTEM §13, U5): [OutOfRange] is the only
 * reason the limit message replaces the context line; [Format] (an extra fraction digit, a second
 * separator, Backspace on empty, a reps separator reachable only from a hardware keyboard) just
 * stops the keystroke and its haptic without implying the value left `min..max`.
 */
enum class KeypadRejection { OutOfRange, Format }

/** The result of applying one [KeypadInput]: the (possibly unchanged) buffer, and why it was
 * refused (`null` = accepted). */
data class KeypadEdit(
    val buffer: KeypadBuffer,
    val rejection: KeypadRejection?,
)
