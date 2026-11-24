package com.repz.app.feature.workout.impl.presentation.picker

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.repz.app.core.designsystem.component.RepzAlertDialog
import com.repz.app.core.designsystem.component.RepzDialogConfirmEmphasis
import com.repz.app.feature.workout.impl.generated.resources.Res
import com.repz.app.feature.workout.impl.generated.resources.exercise_unknown
import com.repz.app.feature.workout.impl.generated.resources.picker_replace_cancel
import com.repz.app.feature.workout.impl.generated.resources.picker_replace_confirm
import com.repz.app.feature.workout.impl.generated.resources.picker_replace_dialog_body
import com.repz.app.feature.workout.impl.generated.resources.picker_replace_dialog_title
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Replace's destructive confirmation (DESIGN_SYSTEM §9): shown only when
 * the replaced card already has a completed set; [RepzAlertDialog]'s Filled-emphasis confirm, no icon. */
@Composable
internal fun ExercisePickerReplaceDialog(
    confirmation: ReplaceConfirmationUi,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val replacedName = confirmation.replacedExerciseName ?: stringResource(Res.string.exercise_unknown)

    RepzAlertDialog(
        title = stringResource(Res.string.picker_replace_dialog_title, replacedName),
        text =
            pluralStringResource(
                Res.plurals.picker_replace_dialog_body,
                confirmation.completedSetCount,
                confirmation.completedSetCount,
            ),
        confirmText = stringResource(Res.string.picker_replace_confirm),
        onConfirm = onConfirm,
        dismissText = stringResource(Res.string.picker_replace_cancel),
        onDismissRequest = onDismiss,
        isConfirmDestructive = true,
        confirmEmphasis = RepzDialogConfirmEmphasis.Filled,
        confirmButtonModifier = Modifier.testTag("picker_replace_confirm"),
        dismissButtonModifier = Modifier.testTag("picker_replace_cancel"),
    )
}
