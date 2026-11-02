package com.repz.app.core.designsystem.haptics

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** MOTION.md §3's 7 haptic events, performed through a platform [RepzHapticPerformer]. */
internal enum class RepzHapticEvent { Tick, Confirm, RestEnd, Success, Reject, DragStart, DragEnd }

/**
 * The platform hook [RepzHaptics] calls into. Android and iOS actuals of
 * [rememberPlatformHapticPerformer] implement this to fire the real haptic for an [RepzHapticEvent].
 */
internal fun interface RepzHapticPerformer {
    fun perform(event: RepzHapticEvent)
}

/**
 * Timings MOTION.md §3 doesn't express as a single haptic call: the gap between RestEnd's two
 * `LONG_PRESS` pulses, and the delay before Success's heavy-click follow-up.
 */
internal object RepzHapticTimings {
    val successHeavyClickDelay: Duration = 120.milliseconds
    val restEndPulseGap: Duration = 120.milliseconds
}
