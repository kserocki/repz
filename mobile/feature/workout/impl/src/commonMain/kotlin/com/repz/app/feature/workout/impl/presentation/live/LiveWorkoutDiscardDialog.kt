package com.repz.app.feature.workout.impl.presentation.live

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.repz.app.core.designsystem.component.RepzAlertDialog
import com.repz.app.core.designsystem.component.RepzDialogConfirmEmphasis
import com.repz.app.core.designsystem.component.RepzIcon
import com.repz.app.feature.workout.impl.generated.resources.Res
import com.repz.app.feature.workout.impl.generated.resources.discard_dialog_body
import com.repz.app.feature.workout.impl.generated.resources.discard_dialog_cancel
import com.repz.app.feature.workout.impl.generated.resources.discard_dialog_confirm
import com.repz.app.feature.workout.impl.generated.resources.discard_dialog_title
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The destructive discard confirm (board 06): shown only once the workout has at least one
 * completed set (0 completed sets discards silently, per
 * [LiveWorkoutLifecycleHandler.onDiscardClicked]). A centred [RepzIcon.DeleteForever] and a
 * Filled-emphasis destructive confirm button, per board 06.
 */
@Composable
internal fun LiveWorkoutDiscardDialog(
    confirmation: DiscardConfirmationUi,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    RepzAlertDialog(
        title = stringResource(Res.string.discard_dialog_title),
        text =
            pluralStringResource(
                Res.plurals.discard_dialog_body,
                confirmation.completedSetCount,
                confirmation.workoutName,
                confirmation.completedSetCount,
            ),
        confirmText = stringResource(Res.string.discard_dialog_confirm),
        onConfirm = onConfirm,
        dismissText = stringResource(Res.string.discard_dialog_cancel),
        onDismissRequest = onDismiss,
        icon = RepzIcon.DeleteForever,
        isConfirmDestructive = true,
        confirmEmphasis = RepzDialogConfirmEmphasis.Filled,
        confirmButtonModifier = Modifier.testTag("discard_confirm"),
        dismissButtonModifier = Modifier.testTag("discard_cancel"),
    )
}
