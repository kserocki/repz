package com.repz.app.core.designsystem.haptics

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.ViewCompat

/**
 * Fires the real Android haptics for each [RepzHapticEvent] (MOTION.md §3): `ViewCompat`'s compat
 * haptic-feedback constants for every single pulse, plus a predefined `Vibrator` heavy click for
 * Success's follow-up, delayed through [View.postDelayed]. Pending delayed pulses are cancelled
 * when the performer leaves composition, so a Success or RestEnd fired just before navigating
 * away never buzzes on a gone screen.
 */
@Composable
internal actual fun rememberPlatformHapticPerformer(): RepzHapticPerformer {
    val view = LocalView.current
    val vibrator = LocalContext.current.systemVibrator()
    val performer = remember(view, vibrator) { AndroidHapticPerformer(view, vibrator) }

    DisposableEffect(performer) { onDispose(performer::cancelPending) }

    return performer
}

private class AndroidHapticPerformer(
    private val view: View,
    private val vibrator: Vibrator?,
) : RepzHapticPerformer {
    private val pendingCallbacks = mutableListOf<Runnable>()

    override fun perform(event: RepzHapticEvent) {
        when (event) {
            RepzHapticEvent.Tick -> view.performHaptic(HapticFeedbackConstantsCompat.CLOCK_TICK)
            RepzHapticEvent.Confirm -> view.performHaptic(HapticFeedbackConstantsCompat.CONFIRM)
            RepzHapticEvent.RestEnd -> performRestEnd()
            RepzHapticEvent.Success -> performSuccess()
            RepzHapticEvent.Reject -> view.performHaptic(HapticFeedbackConstantsCompat.REJECT)
            RepzHapticEvent.DragStart -> view.performHaptic(HapticFeedbackConstantsCompat.GESTURE_START)
            RepzHapticEvent.DragEnd -> view.performHaptic(HapticFeedbackConstantsCompat.GESTURE_END)
        }
    }

    fun cancelPending() {
        pendingCallbacks.forEach(view::removeCallbacks)
        pendingCallbacks.clear()
    }

    private fun performRestEnd() {
        view.performHaptic(HapticFeedbackConstantsCompat.LONG_PRESS)
        postDelayed(RepzHapticTimings.restEndPulseGap.inWholeMilliseconds) {
            view.performHaptic(HapticFeedbackConstantsCompat.LONG_PRESS)
        }
    }

    private fun performSuccess() {
        view.performHaptic(HapticFeedbackConstantsCompat.CONFIRM)
        postDelayed(RepzHapticTimings.successHeavyClickDelay.inWholeMilliseconds) {
            vibrator?.performHeavyClick()
        }
    }

    private fun postDelayed(
        delayMillis: Long,
        action: () -> Unit,
    ) {
        val callback = Runnable(action)
        pendingCallbacks += callback
        view.postDelayed(callback, delayMillis)
    }
}

private fun View.performHaptic(feedbackConstant: Int) {
    ViewCompat.performHapticFeedback(this, feedbackConstant)
}

/** Null when the device or a preview has no vibrator service; the heavy click is then skipped. */
private fun Context.systemVibrator(): Vibrator? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        getSystemService(Vibrator::class.java)
    }

/**
 * `VibrationAttributes.USAGE_TOUCH` only exists from API 33. Below that, the closest equivalent is
 * the `AudioAttributes` pair the platform's own touch-haptics path used before `VibrationAttributes`
 * existed (`CONTENT_TYPE_SONIFICATION` + `USAGE_ASSISTANCE_SONIFICATION`), which API 31-32 map
 * internally to `USAGE_TOUCH` as well.
 */
private val legacyTouchAudioAttributes: AudioAttributes =
    AudioAttributes
        .Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
        .build()

private fun Vibrator.performHeavyClick() {
    if (!hasVibrator()) return
    val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH))
    } else {
        @Suppress("DEPRECATION")
        vibrate(effect, legacyTouchAudioAttributes)
    }
}
