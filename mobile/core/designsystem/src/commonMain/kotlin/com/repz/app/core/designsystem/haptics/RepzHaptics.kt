package com.repz.app.core.designsystem.haptics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
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

/**
 * `core:designsystem`'s haptics API (MOTION.md §3): one method per event. Gated by [enabled] (the
 * `RepzTheme(hapticsEnabled = …)` setting, F23); independent of reduced motion — MOTION.md §3
 * "haptics stay on" — so this class never reads [com.repz.app.core.designsystem.motion.LocalReducedMotion].
 */
@Stable
class RepzHaptics internal constructor(
    private val performer: RepzHapticPerformer,
    private val enabled: Boolean,
) {
    fun tick() = perform(RepzHapticEvent.Tick)

    fun confirm() = perform(RepzHapticEvent.Confirm)

    fun restEnd() = perform(RepzHapticEvent.RestEnd)

    fun success() = perform(RepzHapticEvent.Success)

    fun reject() = perform(RepzHapticEvent.Reject)

    fun dragStart() = perform(RepzHapticEvent.DragStart)

    fun dragEnd() = perform(RepzHapticEvent.DragEnd)

    private fun perform(event: RepzHapticEvent) {
        if (enabled) performer.perform(event)
    }

    companion object {
        val current: RepzHaptics
            @Composable @ReadOnlyComposable
            get() = LocalRepzHaptics.current
    }
}

internal val LocalRepzHaptics =
    staticCompositionLocalOf<RepzHaptics> {
        error("RepzHaptics not provided: wrap the content in RepzTheme.")
    }
