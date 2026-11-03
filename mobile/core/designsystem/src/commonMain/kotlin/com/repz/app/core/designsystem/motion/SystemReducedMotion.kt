package com.repz.app.core.designsystem.motion

import androidx.compose.runtime.Composable

/**
 * Reads the live platform "reduced motion" setting (MOTION.md §2), updating while composed:
 * Android `Settings.Global.ANIMATOR_DURATION_SCALE == 0` (observed through a `ContentObserver`),
 * iOS `UIAccessibilityIsReduceMotionEnabled()` (observed through
 * `UIAccessibilityReduceMotionStatusDidChangeNotification`). [RepzTheme][com.repz.app.core.designsystem.theme.RepzTheme]
 * calls this only when its own `reducedMotion` parameter is `null`.
 */
@Composable
internal expect fun rememberSystemReducedMotion(): Boolean
