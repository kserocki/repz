package com.repz.app.core.designsystem.component.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import com.repz.app.core.designsystem.generated.resources.Res
import com.repz.app.core.designsystem.generated.resources.symbol_delete
import com.repz.app.core.designsystem.haptics.RepzHaptics
import com.repz.app.core.designsystem.theme.RepzIconSize
import com.repz.app.core.designsystem.theme.RepzSpacing
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt

private const val SWIPE_DELETE_THRESHOLD_FRACTION = 0.4f

/**
 * [RepzSetRow]'s drag-to-delete behaviour: [content] follows the finger 1:1, [onDelete] fires past
 * 40 % of the row's width on release (a Tick haptic on crossing), and the content then holds fully
 * off-screen until the caller removes the row, instead of snapping back and flashing visible again.
 * The caller must key each row by a stable id or Compose will reuse this held-off-screen state for
 * whatever row lands at the same position next.
 *
 * TODO(REP-46): replace the instant reveal/commit with MOT-12's real spring and the errorContainer
 * reveal/row-collapse choreography.
 */
@Composable
internal fun SwipeToDeleteBox(
    onDelete: () -> Unit,
    containerColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape = MaterialTheme.shapes.medium
    val haptics = RepzHaptics.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    var rowWidthPx by remember { mutableIntStateOf(0) }
    var isPastDeleteThreshold by remember { mutableStateOf(false) }
    val isRevealed by remember { derivedStateOf { dragOffsetPx < 0f } }

    // propagateMinConstraints: without it, Box relaxes the min height/width it was given (e.g.
    // RepzSetRow's heightIn(min = …)) to 0 for its children, so in an unbounded parent the
    // fillMaxSize()-based children below used to collapse instead of reaching the row's own
    // minimum size.
    Box(modifier = modifier.onSizeChanged { size -> rowWidthPx = size.width }, propagateMinConstraints = true) {
        if (isRevealed) {
            SetRowSwipeBackground(modifier = Modifier.matchParentSize().clip(shape))
        }

        Box(
            modifier =
                Modifier
                    .offset { IntOffset(dragOffsetPx.roundToInt(), 0) }
                    .draggable(
                        orientation = Orientation.Horizontal,
                        reverseDirection = isRtl,
                        state =
                            rememberDraggableState { delta ->
                                dragOffsetPx = (dragOffsetPx + delta).coerceIn(-rowWidthPx.toFloat(), 0f)
                                val threshold = rowWidthPx * SWIPE_DELETE_THRESHOLD_FRACTION
                                val isNowPastThreshold = -dragOffsetPx >= threshold
                                if (isNowPastThreshold != isPastDeleteThreshold) {
                                    isPastDeleteThreshold = isNowPastThreshold
                                    haptics.tick()
                                }
                            },
                        onDragStopped = {
                            if (isPastDeleteThreshold) {
                                onDelete()
                                // Held off-screen rather than reset to 0: snapping back would flash
                                // the row visible again for one frame before the caller's own state
                                // update actually removes it from the list.
                                dragOffsetPx = -rowWidthPx.toFloat()
                            } else {
                                dragOffsetPx = 0f
                            }
                            isPastDeleteThreshold = false
                        },
                    ).clip(shape)
                    .background(containerColor),
        ) {
            content()
        }
    }
}

/**
 * The errorContainer + delete-icon backdrop revealed behind [RepzSetRow] while swiping. `internal`
 * (not `private`) so the preview can reuse the exact same visual instead of redrawing it.
 */
@Composable
internal fun SetRowSwipeBackground(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.errorContainer),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Icon(
            painter = painterResource(Res.drawable.symbol_delete),
            contentDescription = null,
            modifier = Modifier.padding(end = RepzSpacing.lg).size(RepzIconSize.default),
            tint = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}
