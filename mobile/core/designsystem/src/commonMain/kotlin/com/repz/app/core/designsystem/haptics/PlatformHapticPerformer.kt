package com.repz.app.core.designsystem.haptics

import androidx.compose.runtime.Composable

/**
 * Reads the real platform haptic API (MOTION.md §3): Android `ViewCompat.performHapticFeedback`
 * (androidx.core compat constants) plus a `Vibrator` heavy click for Success's follow-up pulse;
 * iOS `UIFeedbackGenerator` subclasses, created once per performer.
 */
@Composable
internal expect fun rememberPlatformHapticPerformer(): RepzHapticPerformer
