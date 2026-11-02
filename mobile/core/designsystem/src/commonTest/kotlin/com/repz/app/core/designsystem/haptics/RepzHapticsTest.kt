package com.repz.app.core.designsystem.haptics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Records every event it's asked to perform, in order, so tests can assert on [events] instead of
 * a real platform API. */
private class RecordingHapticPerformer : RepzHapticPerformer {
    val events = mutableListOf<RepzHapticEvent>()

    override fun perform(event: RepzHapticEvent) {
        events += event
    }
}

class RepzHapticsTest {
    @Test
    fun given_enabled_whenTick_thenPerformerReceivesTick() {
        val performer = RecordingHapticPerformer()
        val haptics = RepzHaptics(performer, enabled = true)

        haptics.tick()

        assertEquals(listOf(RepzHapticEvent.Tick), performer.events)
    }

    @Test
    fun given_enabled_whenEveryMethodIsCalled_thenPerformerReceivesExactlyItsOwnEvent() {
        val performer = RecordingHapticPerformer()
        val haptics = RepzHaptics(performer, enabled = true)

        haptics.confirm()
        haptics.restEnd()
        haptics.success()
        haptics.reject()
        haptics.dragStart()
        haptics.dragEnd()

        assertEquals(
            listOf(
                RepzHapticEvent.Confirm,
                RepzHapticEvent.RestEnd,
                RepzHapticEvent.Success,
                RepzHapticEvent.Reject,
                RepzHapticEvent.DragStart,
                RepzHapticEvent.DragEnd,
            ),
            performer.events,
        )
    }

    @Test
    fun given_disabled_whenAnyMethodIsCalled_thenPerformerReceivesNothing() {
        val performer = RecordingHapticPerformer()
        val haptics = RepzHaptics(performer, enabled = false)

        haptics.tick()
        haptics.confirm()
        haptics.restEnd()
        haptics.success()
        haptics.reject()
        haptics.dragStart()
        haptics.dragEnd()

        assertTrue(performer.events.isEmpty())
    }

    // RepzHaptics(performer, enabled) has no reducedMotion parameter at all (MOTION.md §3 "haptics
    // stay on"), so there is nothing a test could vary: the compiler enforces this, not a case here.
}
