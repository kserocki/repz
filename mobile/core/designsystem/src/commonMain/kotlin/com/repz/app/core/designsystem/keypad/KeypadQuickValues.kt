package com.repz.app.core.designsystem.keypad

import com.repz.app.core.designsystem.format.FixedDecimal

private val WEIGHT_STEP_MULTIPLES = listOf(-2L, -1L, 1L, 2L)
private val REPS_VALUES = listOf(5L, 6L, 8L, 10L, 12L)

/** Quick-value chip suggestions for the keypad's chip row (DESIGN_SYSTEM §9, board 09, plan §3.4). */
object KeypadQuickValues {
    /**
     * [base] ± 1 and ± 2 steps of [step], each rounded half up to a multiple of [step]; drops
     * [base] itself, duplicates and anything outside [min]..[max]; ascending.
     */
    fun weightSuggestions(
        base: FixedDecimal?,
        step: FixedDecimal,
        min: FixedDecimal,
        max: FixedDecimal,
    ): List<FixedDecimal> {
        if (base == null || step.hundredths <= 0) return emptyList()

        return WEIGHT_STEP_MULTIPLES
            .mapNotNull { multiple ->
                val rawHundredths = base.hundredths + multiple * step.hundredths
                if (rawHundredths < 0) null else FixedDecimal(rawHundredths).roundedToMultipleOf(step)
            }.filter { it in min..max && it != base }
            .distinct()
            .sorted()
    }

    /** Fixed reps suggestions (board 09): 5, 6, 8, 10, 12. */
    val repsSuggestions: List<FixedDecimal> = REPS_VALUES.map { FixedDecimal.ofWhole(it) }
}
