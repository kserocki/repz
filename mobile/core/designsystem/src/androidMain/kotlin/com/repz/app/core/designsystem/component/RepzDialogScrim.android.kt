package com.repz.app.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

/** Android's `DialogProperties` (`androidx.compose.ui`, not the Compose Multiplatform skiko one)
 * has no scrim parameter; the dim amount comes from [RepzDialogScrimEffect] instead. */
@Composable
internal actual fun repzDialogProperties(opacity: Float): DialogProperties = DialogProperties()

/** The dialog's `ComposeView` is a [DialogWindowProvider]; its parent is that view, so this must run
 * from inside the dialog's own content, not the screen that opened it. */
@Composable
internal actual fun RepzDialogScrimEffect(opacity: Float) {
    val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
    LaunchedEffect(dialogWindow, opacity) {
        dialogWindow?.setDimAmount(opacity)
    }
}
