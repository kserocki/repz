package com.repz.app.core.designsystem.keypad

import androidx.compose.runtime.saveable.SaverScope
import com.repz.app.core.designsystem.format.FixedDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

private fun requestFor(
    targetKey: String,
    mode: KeypadMode,
): NumberKeypadRequest =
    NumberKeypadRequest(
        targetKey = targetKey,
        field =
            KeypadField(
                mode = mode,
                value = null,
                min = FixedDecimal.ZERO,
                max = FixedDecimal.ofWhole(1_000),
                step = FixedDecimal.ofWhole(1),
                fractionDigits = 2,
                sameAsLast = null,
                quickValues = emptyList(),
                choices = emptyList(),
            ),
        modes = listOf(KeypadMode.Weight, KeypadMode.Reps, KeypadMode.Rpe),
        weightUnitLabel = "kg",
        contextLabel = "Bench Press · Set 3",
        nextAction = KeypadNextAction.Reps,
        stepOptions = emptyList(),
    )

/**
 * [retargetKeyOf] is what [NumberKeypadSheetStateful] compares every recomposition to decide
 * whether to reset its buffer. These tests cover the key's detection logic: it must change
 * whenever either component of the target changes, and stay identical otherwise.
 */
class NumberKeypadSheetStateTest {
    @Test
    fun givenTheSameTargetKeyAndMode_whenComparingRetargetKeys_thenTheyAreEqual() {
        val first = requestFor("catalog-weight:Kg", KeypadMode.Weight)
        val second = requestFor("catalog-weight:Kg", KeypadMode.Weight)

        assertEquals(retargetKeyOf(first), retargetKeyOf(second))
    }

    @Test
    fun givenAUnitSwitchChangingTheTargetKey_whenComparingRetargetKeys_thenTheyDiffer() {
        val kg = requestFor("catalog-weight:Kg", KeypadMode.Weight)
        val lb = requestFor("catalog-weight:Lb", KeypadMode.Weight)

        assertNotEquals(retargetKeyOf(kg), retargetKeyOf(lb))
    }

    @Test
    fun givenAModeSwitchWithTheSameTargetKey_whenComparingRetargetKeys_thenTheyDiffer() {
        val weight = requestFor("catalog-weight:Kg", KeypadMode.Weight)
        val reps = requestFor("catalog-weight:Kg", KeypadMode.Reps)

        assertNotEquals(retargetKeyOf(weight), retargetKeyOf(reps))
    }

    @Test
    fun givenBothTargetKeyAndModeDiffer_whenComparingRetargetKeys_thenTheyDiffer() {
        val first = requestFor("catalog-weight:Kg", KeypadMode.Weight)
        val second = requestFor("catalog-reps", KeypadMode.Reps)

        assertNotEquals(retargetKeyOf(first), retargetKeyOf(second))
    }

    @Test
    fun givenARetargetKeyHolder_whenSavedThenRestored_thenTheValueSurvives() {
        val request = requestFor("catalog-weight:Kg", KeypadMode.Weight)
        val original = LatestValueHolder(retargetKeyOf(request))
        val scope = SaverScope { true }

        val saved = requireNotNull(with(RetargetKeySaver) { scope.save(original) })
        val restored = requireNotNull(RetargetKeySaver.restore(saved))

        assertEquals(original.value, restored.value)
    }
}
