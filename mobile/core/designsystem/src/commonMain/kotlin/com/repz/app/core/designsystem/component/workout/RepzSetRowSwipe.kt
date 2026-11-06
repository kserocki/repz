package com.repz.app.core.designsystem.component.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import com.repz.app.core.designsystem.generated.resources.Res
import com.repz.app.core.designsystem.generated.resources.symbol_delete
import com.repz.app.core.designsystem.theme.RepzIconSize
import com.repz.app.core.designsystem.theme.RepzSpacing
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt

private const val SWIPE_DELETE_THRESHOLD_FRACTION = 0.4f

/**
 * [RepzSetRow]'s drag-to-delete behaviour (MOT-12's reduced-motion end state, Q11-A): [content]
 * follows the finger 1:1 while dragging (no spring) and [onDelete] fires once past 40 % of the
 * row's own width on release; otherwise the offset resets to 0 instantly. A start-to-end drag is
 * clamped to 0 and does nothing.
 */
@Composable
internal fun SwipeToDeleteBox(
    onDelete: () -> Unit,
    containerColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxWithConstraintsScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.medium
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    var rowWidthPx by remember { mutableIntStateOf(0) }

    Box(modifier = modifier.onSizeChanged { size -> rowWidthPx = size.width }) {
        val revealFraction = if (rowWidthPx > 0) (-dragOffsetPx / rowWidthPx).coerceIn(0f, 1f) else 0f
        if (revealFraction > 0f) {
            SetRowSwipeBackground(modifier = Modifier.fillMaxSize().clip(shape))
        }

        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxSize()
                    .offset { IntOffset(dragOffsetPx.roundToInt(), 0) }
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state =
                            rememberDraggableState { delta ->
                                dragOffsetPx = (dragOffsetPx + delta).coerceIn(-rowWidthPx.toFloat(), 0f)
                            },
                        onDragStopped = {
                            val threshold = rowWidthPx * SWIPE_DELETE_THRESHOLD_FRACTION
                            if (-dragOffsetPx >= threshold) onDelete()
                            dragOffsetPx = 0f
                        },
                    ).clip(shape)
                    .background(containerColor),
            content = content,
        )
    }
}

@Composable
private fun SetRowSwipeBackground(modifier: Modifier = Modifier) {
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
