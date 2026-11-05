package com.repz.app.core.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogProperties

/** Compose Multiplatform's iOS `DialogProperties` draws the scrim itself. */
@Composable
internal actual fun repzDialogProperties(opacity: Float): DialogProperties =
    DialogProperties(scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = opacity))

/** No-op: [repzDialogProperties] already draws the scrim on iOS. */
@Composable
internal actual fun RepzDialogScrimEffect(opacity: Float) = Unit
