package com.repz.app.core.designsystem.motion

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * Tracks `Settings.Global.ANIMATOR_DURATION_SCALE` live through a [ContentObserver] registered on
 * the setting's own URI. Only a scale of exactly 0 ("Remove animations") counts as reduced motion;
 * every other scale is already applied by Compose's own animation clock (MOTION.md §2, Q4).
 */
@Composable
internal actual fun rememberSystemReducedMotion(): Boolean {
    val context = LocalContext.current
    var reducedMotion by remember { mutableStateOf(context.hasZeroAnimatorDurationScale()) }

    DisposableEffect(context) {
        val observer =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    reducedMotion = context.hasZeroAnimatorDurationScale()
                }
            }
        context.contentResolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer,
        )
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }

    return reducedMotion
}

private fun Context.hasZeroAnimatorDurationScale(): Boolean =
    Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
