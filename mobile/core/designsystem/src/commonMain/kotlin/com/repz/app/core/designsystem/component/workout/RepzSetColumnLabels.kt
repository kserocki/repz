package com.repz.app.core.designsystem.component.workout

import androidx.compose.runtime.Immutable

/** Column header labels above a card's [RepzSetRow]s, aligned to the same columns (DESIGN_SYSTEM
 * §9 board 09: "Set", "Prev", the weight unit, "Reps"). */
@Immutable
data class RepzSetColumnLabels(
    val set: String,
    val previous: String,
    val weight: String,
    val reps: String,
)
