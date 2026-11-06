package com.repz.app.core.designsystem.component.workout

import androidx.compose.runtime.Immutable

/**
 * A set's type (DESIGN_SYSTEM §9, board 05): badge letters W / numbered / D / F. Colours come from
 * `RepzTheme.colors.setX`/`onSetX`/`setXContainer`/`onSetXContainer` (§3.2).
 */
enum class RepzSetType { Warmup, Normal, Drop, Failure }

/** Which [RepzSetRow] value field is being edited (DESIGN_SYSTEM §9 `SetRow` "editing" state). */
enum class RepzSetField { Weight, Reps }

/**
 * One row of a menu internal to a live-workout component (the set-type badge menu, a card's
 * overflow menu, the header's overflow menu): these components own their menus, so this isn't a
 * generic `core:designsystem` menu API (DESIGN_SYSTEM §9 "Landed" note).
 */
@Immutable
data class RepzMenuItem(
    val label: String,
    val onClick: () -> Unit,
    val isDestructive: Boolean = false,
    val isChecked: Boolean = false,
)

/**
 * One custom accessibility action (DESIGN_SYSTEM §12: "Delete set", "Mark as
 * warm-up/drop/failure", "Move up/down").
 */
@Immutable
data class RepzAccessibilityAction(
    val label: String,
    val onAction: () -> Unit,
)
