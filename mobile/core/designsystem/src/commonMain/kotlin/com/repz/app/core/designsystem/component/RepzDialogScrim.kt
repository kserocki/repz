package com.repz.app.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogProperties

/**
 * [DialogProperties] for [opacity] (DESIGN_SYSTEM §7, `#000` scrim): on iOS, Compose Multiplatform's
 * own `DialogProperties(scrimColor = …)` draws it directly. On Android that constructor parameter
 * doesn't exist (its `DialogProperties` is the plain `androidx.compose.ui` one); Android gets the
 * same opacity instead through [RepzDialogScrimEffect], which calls `Window.setDimAmount` on the
 * dialog's own window.
 */
@Composable
internal expect fun repzDialogProperties(opacity: Float): DialogProperties

/**
 * Applies [opacity] as the dialog window's dim amount on Android (DESIGN_SYSTEM §7); a no-op on
 * platforms where [repzDialogProperties] already draws the scrim itself. Call this once, inside one
 * of the dialog's own content slots (so `LocalView.current` resolves to the dialog's window, not the
 * screen behind it).
 */
@Composable
internal expect fun RepzDialogScrimEffect(opacity: Float)
