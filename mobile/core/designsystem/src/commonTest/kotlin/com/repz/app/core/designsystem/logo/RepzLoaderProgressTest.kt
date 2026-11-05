package com.repz.app.core.designsystem.logo

import kotlin.test.Test
import kotlin.test.assertEquals

class RepzLoaderProgressTest {
    @Test
    fun given_progressZero_whenLockedPairCount_thenZero() {
        assertEquals(0, lockedPairCount(0f))
    }

    @Test
    fun given_progressJustBelowOneThird_whenLockedPairCount_thenZero() {
        assertEquals(0, lockedPairCount(0.33f))
    }

    @Test
    fun given_progressJustAtOneThird_whenLockedPairCount_thenOne() {
        assertEquals(1, lockedPairCount(0.34f))
    }

    @Test
    fun given_progressJustBelowTwoThirds_whenLockedPairCount_thenOne() {
        assertEquals(1, lockedPairCount(0.66f))
    }

    @Test
    fun given_progressJustAtTwoThirds_whenLockedPairCount_thenTwo() {
        assertEquals(2, lockedPairCount(0.67f))
    }

    @Test
    fun given_progressJustBelowOne_whenLockedPairCount_thenTwo() {
        assertEquals(2, lockedPairCount(0.99f))
    }

    @Test
    fun given_progressOne_whenLockedPairCount_thenThree() {
        assertEquals(3, lockedPairCount(1f))
    }

    @Test
    fun given_progressBelowZero_whenLockedPairCount_thenCoercedToZero() {
        assertEquals(0, lockedPairCount(-0.5f))
    }

    @Test
    fun given_progressAboveOne_whenLockedPairCount_thenCoercedToThree() {
        assertEquals(3, lockedPairCount(1.5f))
    }

    @Test
    fun given_progressIsNaN_whenLockedPairCount_thenZero() {
        assertEquals(0, lockedPairCount(Float.NaN))
    }

    @Test
    fun given_progressZero_whenPairStates_thenInnerIsLoadingAndTheRestArePending() {
        assertEquals(
            mapOf(
                RepzPlatePair.Inner to PairState.Loading,
                RepzPlatePair.Middle to PairState.Pending,
                RepzPlatePair.Outer to PairState.Pending,
            ),
            pairStates(0f),
        )
    }

    @Test
    fun given_progressOneThird_whenPairStates_thenInnerIsLockedAndMiddleIsLoading() {
        assertEquals(
            mapOf(
                RepzPlatePair.Inner to PairState.Locked,
                RepzPlatePair.Middle to PairState.Loading,
                RepzPlatePair.Outer to PairState.Pending,
            ),
            pairStates(0.34f),
        )
    }

    @Test
    fun given_progressTwoThirds_whenPairStates_thenInnerAndMiddleAreLockedAndOuterIsLoading() {
        assertEquals(
            mapOf(
                RepzPlatePair.Inner to PairState.Locked,
                RepzPlatePair.Middle to PairState.Locked,
                RepzPlatePair.Outer to PairState.Loading,
            ),
            pairStates(0.67f),
        )
    }

    @Test
    fun given_progressOne_whenPairStates_thenEveryPairIsLockedAndNoneIsLoading() {
        assertEquals(
            mapOf(
                RepzPlatePair.Inner to PairState.Locked,
                RepzPlatePair.Middle to PairState.Locked,
                RepzPlatePair.Outer to PairState.Locked,
            ),
            pairStates(1f),
        )
    }
}
