package com.repz.app.core.ui.label

import androidx.compose.runtime.Composable
import com.repz.app.core.domain.model.Equipment
import com.repz.app.core.ui.generated.resources.Res
import com.repz.app.core.ui.generated.resources.equipment_band
import com.repz.app.core.ui.generated.resources.equipment_barbell
import com.repz.app.core.ui.generated.resources.equipment_bodyweight
import com.repz.app.core.ui.generated.resources.equipment_cable
import com.repz.app.core.ui.generated.resources.equipment_dumbbell
import com.repz.app.core.ui.generated.resources.equipment_kettlebell
import com.repz.app.core.ui.generated.resources.equipment_machine
import com.repz.app.core.ui.generated.resources.equipment_other
import org.jetbrains.compose.resources.stringResource

/** The localized display label for an [Equipment] (DESIGN_SYSTEM §9): the exercise row/card
 * subtitle's "<primary muscle> · <equipment>" and anywhere else equipment needs showing to the
 * user. */
@Composable
fun Equipment.label(): String =
    when (this) {
        Equipment.BARBELL -> stringResource(Res.string.equipment_barbell)
        Equipment.DUMBBELL -> stringResource(Res.string.equipment_dumbbell)
        Equipment.KETTLEBELL -> stringResource(Res.string.equipment_kettlebell)
        Equipment.MACHINE -> stringResource(Res.string.equipment_machine)
        Equipment.CABLE -> stringResource(Res.string.equipment_cable)
        Equipment.BODYWEIGHT -> stringResource(Res.string.equipment_bodyweight)
        Equipment.BAND -> stringResource(Res.string.equipment_band)
        Equipment.OTHER -> stringResource(Res.string.equipment_other)
    }
