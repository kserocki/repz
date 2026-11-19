package com.repz.app.core.ui.label

import androidx.compose.runtime.Composable
import com.repz.app.core.domain.model.Muscle
import com.repz.app.core.ui.generated.resources.Res
import com.repz.app.core.ui.generated.resources.muscle_abs
import com.repz.app.core.ui.generated.resources.muscle_back
import com.repz.app.core.ui.generated.resources.muscle_biceps
import com.repz.app.core.ui.generated.resources.muscle_calves
import com.repz.app.core.ui.generated.resources.muscle_chest
import com.repz.app.core.ui.generated.resources.muscle_forearms
import com.repz.app.core.ui.generated.resources.muscle_full_body
import com.repz.app.core.ui.generated.resources.muscle_glutes
import com.repz.app.core.ui.generated.resources.muscle_hamstrings
import com.repz.app.core.ui.generated.resources.muscle_quadriceps
import com.repz.app.core.ui.generated.resources.muscle_shoulders
import com.repz.app.core.ui.generated.resources.muscle_triceps
import org.jetbrains.compose.resources.stringResource

/** The localized display label for a [Muscle] (DESIGN_SYSTEM §9): the exercise card subtitle's
 * "<primary muscle> · n of m done" and anywhere else a muscle group needs showing to the user. */
@Composable
fun Muscle.label(): String =
    when (this) {
        Muscle.CHEST -> stringResource(Res.string.muscle_chest)
        Muscle.BACK -> stringResource(Res.string.muscle_back)
        Muscle.SHOULDERS -> stringResource(Res.string.muscle_shoulders)
        Muscle.BICEPS -> stringResource(Res.string.muscle_biceps)
        Muscle.TRICEPS -> stringResource(Res.string.muscle_triceps)
        Muscle.FOREARMS -> stringResource(Res.string.muscle_forearms)
        Muscle.ABS -> stringResource(Res.string.muscle_abs)
        Muscle.GLUTES -> stringResource(Res.string.muscle_glutes)
        Muscle.QUADRICEPS -> stringResource(Res.string.muscle_quadriceps)
        Muscle.HAMSTRINGS -> stringResource(Res.string.muscle_hamstrings)
        Muscle.CALVES -> stringResource(Res.string.muscle_calves)
        Muscle.FULL_BODY -> stringResource(Res.string.muscle_full_body)
    }
