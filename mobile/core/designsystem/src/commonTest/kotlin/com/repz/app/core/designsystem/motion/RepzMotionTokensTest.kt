package com.repz.app.core.designsystem.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val SAMPLE_FRACTIONS = listOf(0.25f, 0.5f, 0.75f)

class RepzMotionTokensTest {
    @Test
    fun given_repzDurations_whenReadingEveryToken_thenMatchesMotionDoc() {
        assertEquals(100.milliseconds, RepzDurations.short)
        assertEquals(200.milliseconds, RepzDurations.medium)
        assertEquals(280.milliseconds, RepzDurations.pathDraw)
        assertEquals(600.milliseconds, RepzDurations.chartDraw)
        assertEquals(1.seconds, RepzDurations.restRingStep)
        assertEquals(600.milliseconds, RepzDurations.particleLifeInline)
        assertEquals(1200.milliseconds, RepzDurations.particleLifeSummary)
        assertEquals(1500.milliseconds, RepzDurations.celebrationMax)
        assertEquals(1200.milliseconds, RepzDurations.shimmer)
        assertEquals(150.milliseconds, RepzDurations.reducedMotionFade)
    }

    @Test
    fun given_repzStagger_whenReadingEveryToken_thenMatchesMotionDoc() {
        assertEquals(110.milliseconds, RepzStagger.logoPlatesPerPair)
        assertEquals(30.milliseconds, RepzStagger.listPerItem)
        assertEquals(8, RepzStagger.LIST_MAX_ITEMS)
        assertEquals(70.milliseconds, RepzStagger.summaryPerBlock)
        assertEquals(12.milliseconds, RepzStagger.heatmapPerDiagonal)
        assertEquals(20.milliseconds, RepzStagger.digitsPerDigit)
    }

    @Test
    fun given_emphasizedEasing_whenSampled_thenMatchesCubicBezier0_2_0_0_1() {
        val reference = CubicBezierEasing(0.2f, 0f, 0f, 1f)
        SAMPLE_FRACTIONS.forEach { fraction -> assertEquals(reference.transform(fraction), RepzEasing.emphasized.transform(fraction)) }
    }

    @Test
    fun given_emphasizedDecelerateEasing_whenSampled_thenMatchesCubicBezier0_05_0_7_0_1_1() {
        val reference = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
        SAMPLE_FRACTIONS.forEach { fraction ->
            assertEquals(reference.transform(fraction), RepzEasing.emphasizedDecelerate.transform(fraction))
        }
    }

    @Test
    fun given_linearEasing_whenSampled_thenMatchesLinearEasing() {
        SAMPLE_FRACTIONS.forEach { fraction -> assertEquals(LinearEasing.transform(fraction), RepzEasing.linear.transform(fraction)) }
    }
}
