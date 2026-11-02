package com.repz.app.core.designsystem.motion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled
import platform.UIKit.UIAccessibilityReduceMotionStatusDidChangeNotification

/**
 * Reads `UIAccessibilityIsReduceMotionEnabled()` once, then keeps it live by observing
 * `UIAccessibilityReduceMotionStatusDidChangeNotification` on the main queue for as long as this
 * is composed.
 */
@Composable
internal actual fun rememberSystemReducedMotion(): Boolean {
    var reducedMotion by remember { mutableStateOf(UIAccessibilityIsReduceMotionEnabled()) }

    DisposableEffect(Unit) {
        val observer =
            NSNotificationCenter.defaultCenter.addObserverForName(
                name = UIAccessibilityReduceMotionStatusDidChangeNotification,
                `object` = null,
                queue = NSOperationQueue.mainQueue,
            ) {
                reducedMotion = UIAccessibilityIsReduceMotionEnabled()
            }

        onDispose {
            NSNotificationCenter.defaultCenter.removeObserver(observer)
        }
    }

    return reducedMotion
}
