package com.repz.app.core.designsystem.component.workout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp

/**
 * A set's type (DESIGN_SYSTEM §9, board 05): badge letters W / numbered / D / F. Colours come from
 * `RepzTheme.colors.setX`/`onSetX`/`setXContainer`/`onSetXContainer`.
 */
enum class RepzSetType { Warmup, Normal, Drop, Failure }

// DESIGN_SYSTEM §9 SetRow: the Previous column hides below this WINDOW width, not a row's own,
// narrower width once a card's padding is subtracted.
private val NarrowWindowWidthThreshold = 360.dp

/** Whether [RepzSetRow]'s Previous column (and the matching header label) shows: computed once from
 * the window's own width and from [REFLOW_FONT_SCALE_THRESHOLD], so [RepzExerciseCard] and every
 * row it contains always agree instead of each measuring its own, possibly different, width. */
@Composable
internal fun rememberIsSetPreviousColumnVisible(): Boolean {
    val density = LocalDensity.current
    val windowWidthPx = LocalWindowInfo.current.containerSize.width
    return remember(density, windowWidthPx) {
        with(density) { windowWidthPx.toDp() } >= NarrowWindowWidthThreshold && density.fontScale < REFLOW_FONT_SCALE_THRESHOLD
    }
}

/** Which [RepzSetRow] value field is being edited (DESIGN_SYSTEM §9 `SetRow` "editing" state). */
enum class RepzSetField { Weight, Reps }

/**
 * One row of a menu internal to a live-workout component (the set-type badge menu, a card's
 * overflow menu, the header's overflow menu): these components own their menus, so this isn't a
 * generic `core:designsystem` menu API (DESIGN_SYSTEM §9 "Landed" note). [modifier] tags this one
 * row (e.g. a test tag), since the menu itself has no per-row slot a caller could reach otherwise.
 */
@Immutable
data class RepzMenuItem(
    val label: String,
    val onClick: () -> Unit,
    val isDestructive: Boolean = false,
    val isChecked: Boolean = false,
    val modifier: Modifier = Modifier,
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
