package com.repz.app.core.domain.record

import com.repz.app.core.domain.model.Weight
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private const val MAX_GRID_KILOGRAMS = 300.0
private const val GRID_STEP_KILOGRAMS = 2.5
private const val MAX_GRID_REPS = 30

class OneRepMaxTest {
    @Test
    fun givenOneRep_whenEstimated_thenTheWeightItselfIsTheOneRepMax() {
        val weight = Weight.fromKilograms(82.5)

        assertEquals(weight, estimateOneRepMax(weight, reps = 1))
    }

    @Test
    fun given100KgForTenReps_whenEstimated_thenItMatchesEpley() {
        assertEquals(Weight(133_333), estimateOneRepMax(Weight.fromKilograms(100.0), reps = 10))
    }

    @Test
    fun given82Point5KgForEightReps_whenEstimated_thenItMatchesEpley() {
        assertEquals(Weight(104_500), estimateOneRepMax(Weight.fromKilograms(82.5), reps = 8))
    }

    @Test
    fun givenAHalfGramResult_whenEstimated_thenItRoundsUp() {
        // Epley(1 g, 15 reps) = 1 * (1 + 15/30) = 1.5 g, a tie that must round up.
        assertEquals(Weight(2), estimateOneRepMax(Weight(1), reps = 15))
    }

    @Test
    fun givenAGridOfWeightsAndReps_whenEstimated_thenItIsMonotonicAndNeverBelowTheWeight() {
        val weights =
            generateSequence(0.0) { it + GRID_STEP_KILOGRAMS }
                .takeWhile { it <= MAX_GRID_KILOGRAMS }
                .map { Weight.fromKilograms(it) }
                .toList()

        for (reps in 1..MAX_GRID_REPS) {
            var previousAcrossWeights: Weight? = null
            weights.forEach { weight ->
                val estimate = estimateOneRepMax(weight, reps)

                assertTrue(estimate >= weight, "estimate must be >= the weight")
                previousAcrossWeights?.let { previous -> assertTrue(estimate >= previous, "must not decrease as weight increases") }
                previousAcrossWeights = estimate
            }
        }

        weights.forEach { weight ->
            var previousAcrossReps: Weight? = null
            for (reps in 1..MAX_GRID_REPS) {
                val estimate = estimateOneRepMax(weight, reps)
                previousAcrossReps?.let { previous -> assertTrue(estimate >= previous, "must not decrease as reps increase") }
                previousAcrossReps = estimate
            }
        }
    }

    @Test
    fun givenZeroReps_whenEstimated_thenRequireFails() {
        assertFailsWith<IllegalArgumentException> { estimateOneRepMax(Weight.fromKilograms(100.0), reps = 0) }
    }
}
