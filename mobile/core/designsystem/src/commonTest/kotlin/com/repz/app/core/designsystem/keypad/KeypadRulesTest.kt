package com.repz.app.core.designsystem.keypad

import com.repz.app.core.designsystem.format.FixedDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun weightField(
    value: FixedDecimal? = null,
    min: FixedDecimal = FixedDecimal.ZERO,
    max: FixedDecimal = FixedDecimal(100_000),
    step: FixedDecimal = FixedDecimal(250),
) = KeypadField(
    mode = KeypadMode.Weight,
    value = value,
    min = min,
    max = max,
    step = step,
    fractionDigits = 2,
    sameAsLast = null,
    quickValues = emptyList(),
    choices = emptyList(),
)

private fun repsField(
    value: FixedDecimal? = null,
    max: FixedDecimal = FixedDecimal(99_900),
) = KeypadField(
    mode = KeypadMode.Reps,
    value = value,
    min = FixedDecimal.ZERO,
    max = max,
    step = FixedDecimal(100),
    fractionDigits = 0,
    sameAsLast = null,
    quickValues = emptyList(),
    choices = emptyList(),
)

private fun rpeField(value: FixedDecimal? = null) =
    KeypadField(
        mode = KeypadMode.Rpe,
        value = value,
        min = FixedDecimal(600),
        max = FixedDecimal(1_000),
        step = FixedDecimal(50),
        fractionDigits = 1,
        sameAsLast = null,
        quickValues = emptyList(),
        choices = emptyList(),
    )

class KeypadRulesTest {
    @Test
    fun givenANullValue_whenBuildingABuffer_thenItIsEmptyAndUntouched() {
        val buffer = KeypadRules.bufferFor(null)

        assertEquals(KeypadBuffer("", isUntouched = true), buffer)
    }

    @Test
    fun givenAnEmptyBuffer_whenReadingTheValue_thenItIsNull() {
        assertNull(KeypadRules.valueOf(KeypadBuffer("", isUntouched = true)))
    }

    @Test
    fun givenABufferEndingInASeparator_whenReadingTheValue_thenTheFractionIsZero() {
        assertEquals(FixedDecimal(8_200), KeypadRules.valueOf(KeypadBuffer("82.", isUntouched = false)))
    }

    @Test
    fun givenAWholeNumberBuffer_whenReadingTheValue_thenItHasNoFraction() {
        assertEquals(FixedDecimal(13_500), KeypadRules.valueOf(KeypadBuffer("135", isUntouched = false)))
    }

    @Test
    fun givenAnUntouchedBuffer_whenADigitIsPressed_thenItReplacesThePreviousValue() {
        val buffer = KeypadRules.bufferFor(FixedDecimal(8_250)) // "82.5", untouched

        val edit = KeypadRules.apply(buffer, KeypadInput.Digit(5), weightField())

        assertFalse(edit.isRejected)
        assertEquals("5", edit.buffer.text)
        assertFalse(edit.buffer.isUntouched)
    }

    @Test
    fun givenAnUntouchedBuffer_whenBackspaceIsPressed_thenItDeletesTheLastCharacterAndBecomesTouched() {
        val buffer = KeypadRules.bufferFor(FixedDecimal(8_250)) // "82.5", untouched

        val edit = KeypadRules.apply(buffer, KeypadInput.Backspace, weightField())

        assertFalse(edit.isRejected)
        assertEquals("82.", edit.buffer.text)
        assertFalse(edit.buffer.isUntouched)
    }

    @Test
    fun givenABufferWithASeparator_whenAnotherSeparatorIsPressed_thenItIsRejected() {
        val buffer = KeypadBuffer("82.", isUntouched = false)

        val edit = KeypadRules.apply(buffer, KeypadInput.DecimalSeparator, weightField())

        assertTrue(edit.isRejected)
        assertEquals(buffer, edit.buffer)
    }

    @Test
    fun givenTwoFractionDigitsAlready_whenAThirdDigitIsPressed_thenItIsRejected() {
        val buffer = KeypadBuffer("82.50", isUntouched = false)

        val edit = KeypadRules.apply(buffer, KeypadInput.Digit(5), weightField())

        assertTrue(edit.isRejected)
        assertEquals(buffer, edit.buffer)
    }

    @Test
    fun givenRepsMode_whenTheSeparatorIsPressed_thenItIsRejected() {
        val buffer = KeypadBuffer("12", isUntouched = false)

        val edit = KeypadRules.apply(buffer, KeypadInput.DecimalSeparator, repsField())

        assertTrue(edit.isRejected)
        assertEquals(buffer, edit.buffer)
    }

    @Test
    fun givenASingleLeadingZero_whenANonZeroDigitIsPressed_thenTheLeadingZeroIsDropped() {
        val buffer = KeypadBuffer("0", isUntouched = false)

        val edit = KeypadRules.apply(buffer, KeypadInput.Digit(5), weightField())

        assertFalse(edit.isRejected)
        assertEquals("5", edit.buffer.text)
    }

    @Test
    fun givenAValueNearTheLimit_whenADigitWouldExceedTheMaximum_thenItIsRejected() {
        val buffer = KeypadBuffer("100", isUntouched = false) // max is 1000.00; "1001" would be 1001.00

        val edit = KeypadRules.apply(buffer, KeypadInput.Digit(1), weightField())

        assertTrue(edit.isRejected)
        assertEquals(buffer, edit.buffer)
    }

    @Test
    fun givenAnOffGridValue_whenIncreased_thenItStepsByTheCurrentStepWithoutSnapping() {
        val buffer = KeypadBuffer("83", isUntouched = false)

        val edit = KeypadRules.apply(buffer, KeypadInput.Increase, weightField())

        assertFalse(edit.isRejected)
        assertEquals(FixedDecimal(8_550), KeypadRules.valueOf(edit.buffer))
        assertTrue(edit.buffer.isUntouched)
    }

    @Test
    fun givenAValueNearTheMaximum_whenIncreased_thenItClampsToTheMaximum() {
        val buffer = KeypadBuffer("999", isUntouched = false)

        val edit = KeypadRules.apply(buffer, KeypadInput.Increase, weightField())

        assertFalse(edit.isRejected)
        assertEquals(FixedDecimal(100_000), KeypadRules.valueOf(edit.buffer))
    }

    @Test
    fun givenAValueAtTheMaximum_whenIncreasedAgain_thenItIsRejected() {
        val buffer = KeypadBuffer("1000", isUntouched = false)

        val edit = KeypadRules.apply(buffer, KeypadInput.Increase, weightField())

        assertTrue(edit.isRejected)
        assertEquals(buffer, edit.buffer)
    }

    @Test
    fun givenAValueAtTheMinimum_whenDecreased_thenItIsRejected() {
        val buffer = KeypadBuffer("0", isUntouched = false)

        val edit = KeypadRules.apply(buffer, KeypadInput.Decrease, weightField())

        assertTrue(edit.isRejected)
        assertEquals(buffer, edit.buffer)
    }

    @Test
    fun givenRpeAtItsMinimum_whenDecreased_thenItIsRejected() {
        val buffer = KeypadRules.bufferFor(FixedDecimal(600)) // "6"

        val edit = KeypadRules.apply(buffer, KeypadInput.Decrease, rpeField())

        assertTrue(edit.isRejected)
    }

    @Test
    fun givenAnEmptyBuffer_whenIncreased_thenItGivesTheMinimumPlusTheStep() {
        val edit = KeypadRules.apply(KeypadBuffer("", isUntouched = true), KeypadInput.Increase, weightField())

        assertFalse(edit.isRejected)
        assertEquals(FixedDecimal(250), KeypadRules.valueOf(edit.buffer))
    }

    @Test
    fun givenAnEmptyRpeBuffer_whenIncreased_thenItGivesTheMinimum() {
        val edit = KeypadRules.apply(KeypadBuffer("", isUntouched = true), KeypadInput.Increase, rpeField())

        assertFalse(edit.isRejected)
        assertEquals(FixedDecimal(600), KeypadRules.valueOf(edit.buffer))
    }

    @Test
    fun givenAnEmptyBuffer_whenDecreased_thenItIsRejected() {
        val edit = KeypadRules.apply(KeypadBuffer("", isUntouched = true), KeypadInput.Decrease, weightField())

        assertTrue(edit.isRejected)
    }

    @Test
    fun givenASetInput_whenApplied_thenTheBufferIsReplacedAndMarkedUntouched() {
        val edit = KeypadRules.apply(KeypadBuffer("12", isUntouched = false), KeypadInput.Set(FixedDecimal(8_250)), weightField())

        assertFalse(edit.isRejected)
        assertTrue(edit.buffer.isUntouched)
        assertEquals(FixedDecimal(8_250), KeypadRules.valueOf(edit.buffer))
    }

    @Test
    fun givenRpeMode_whenADigitIsPressed_thenItIsIgnored() {
        val buffer = KeypadRules.bufferFor(FixedDecimal(800)) // "8"

        val edit = KeypadRules.apply(buffer, KeypadInput.Digit(5), rpeField())

        assertFalse(edit.isRejected)
        assertEquals(buffer, edit.buffer)
    }

    @Test
    fun givenRpeMode_whenTheSeparatorIsPressed_thenItIsIgnored() {
        val buffer = KeypadRules.bufferFor(FixedDecimal(800))

        val edit = KeypadRules.apply(buffer, KeypadInput.DecimalSeparator, rpeField())

        assertFalse(edit.isRejected)
        assertEquals(buffer, edit.buffer)
    }

    @Test
    fun givenRpeMode_whenBackspaceIsPressed_thenItIsIgnored() {
        val buffer = KeypadRules.bufferFor(FixedDecimal(800))

        val edit = KeypadRules.apply(buffer, KeypadInput.Backspace, rpeField())

        assertFalse(edit.isRejected)
        assertEquals(buffer, edit.buffer)
    }

    @Test
    fun givenAnyValue_whenCleared_thenItBecomesEmptyAndUntouched() {
        val edit = KeypadRules.apply(KeypadBuffer("82.5", isUntouched = false), KeypadInput.Clear, weightField())

        assertFalse(edit.isRejected)
        assertEquals(KeypadBuffer("", isUntouched = true), edit.buffer)
    }

    @Test
    fun givenAnRpeValue_whenCleared_thenItBecomesEmpty() {
        val buffer = KeypadRules.bufferFor(FixedDecimal(800))

        val edit = KeypadRules.apply(buffer, KeypadInput.Clear, rpeField())

        assertFalse(edit.isRejected)
        assertEquals(KeypadBuffer("", isUntouched = true), edit.buffer)
    }
}
