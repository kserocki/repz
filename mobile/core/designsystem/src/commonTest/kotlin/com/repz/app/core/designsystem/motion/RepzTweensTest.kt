package com.repz.app.core.designsystem.motion

import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.TweenSpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RepzTweensTest {
    @Test
    fun given_notReduced_whenReadingEveryTween_thenReturnsTheMotionDocDurations() {
        val tweens = RepzTweens(reducedMotion = false)

        assertEquals(TweenSpec<Float>(durationMillis = 100), tweens.short())
        assertEquals(TweenSpec<Float>(durationMillis = 200), tweens.medium())
        assertEquals(TweenSpec<Float>(durationMillis = 280, easing = RepzEasing.emphasizedDecelerate), tweens.pathDraw())
        assertEquals(TweenSpec<Float>(durationMillis = 600, easing = RepzEasing.emphasizedDecelerate), tweens.chartDraw())
    }

    @Test
    fun given_reduced_whenReadingShort_thenStaysA100MsTween() {
        val tweens = RepzTweens(reducedMotion = true)

        assertEquals(TweenSpec<Float>(durationMillis = 100), tweens.short())
    }

    @Test
    fun given_reduced_whenReadingMedium_thenShortensTo150Ms() {
        val tweens = RepzTweens(reducedMotion = true)

        assertEquals(TweenSpec<Float>(durationMillis = 150), tweens.medium())
    }

    @Test
    fun given_reduced_whenReadingTheDrawTweens_thenReturnsASnapSpec() {
        val tweens = RepzTweens(reducedMotion = true)

        assertIs<SnapSpec<Float>>(tweens.pathDraw<Float>())
        assertIs<SnapSpec<Float>>(tweens.chartDraw<Float>())
    }
}
