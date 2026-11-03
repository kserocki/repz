package com.repz.app.core.designsystem.haptics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UISelectionFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType.UINotificationFeedbackTypeError as FeedbackError
import platform.UIKit.UINotificationFeedbackType.UINotificationFeedbackTypeSuccess as FeedbackSuccess
import platform.UIKit.UINotificationFeedbackType.UINotificationFeedbackTypeWarning as FeedbackWarning

@Composable
internal actual fun rememberPlatformHapticPerformer(): RepzHapticPerformer = remember { IosHapticPerformer() }

/**
 * Maps MOTION.md §3's 7 events to the three `UIFeedbackGenerator` subclasses. Each generator is
 * created once per performer and only ever called on the main thread (Compose's own).
 */
private class IosHapticPerformer : RepzHapticPerformer {
    private val selection = UISelectionFeedbackGenerator()
    private val notification = UINotificationFeedbackGenerator()
    private val lightImpact = UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleLight)
    private val mediumImpact = UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium)

    override fun perform(event: RepzHapticEvent) {
        when (event) {
            RepzHapticEvent.Tick -> selection.selectionChanged()
            RepzHapticEvent.Confirm -> lightImpact.impactOccurred()
            RepzHapticEvent.RestEnd -> notification.notificationOccurred(FeedbackWarning)
            RepzHapticEvent.Success -> notification.notificationOccurred(FeedbackSuccess)
            RepzHapticEvent.Reject -> notification.notificationOccurred(FeedbackError)
            RepzHapticEvent.DragStart, RepzHapticEvent.DragEnd -> mediumImpact.impactOccurred()
        }
    }
}
