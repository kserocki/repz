package com.repz.app.core.designsystem.component.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.repz.app.core.designsystem.generated.resources.Res
import com.repz.app.core.designsystem.generated.resources.symbol_check_circle_filled
import com.repz.app.core.designsystem.generated.resources.symbol_drag_indicator
import com.repz.app.core.designsystem.theme.RepzElevation
import com.repz.app.core.designsystem.theme.RepzIconSize
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.designsystem.theme.RepzTheme
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt

// DESIGN_SYSTEM §9, board 09 §10b: reorder rows collapse to 64 dp.
private val ReorderRowHeight = 64.dp

/** One exercise in [RepzExerciseReorderList] (DESIGN_SYSTEM §9, board 09 §10b). [moveUp]/[moveDown]
 * back the "Move up"/"Move down" accessibility actions (`null` at the respective end of the list). */
@Immutable
data class RepzReorderItem<K : Any>(
    val key: K,
    val title: String,
    val subtitle: String,
    val isDone: Boolean,
    val moveUp: RepzAccessibilityAction?,
    val moveDown: RepzAccessibilityAction?,
)

/**
 * The reorder-mode list (DESIGN_SYSTEM §9, board 09 §10b; T2): a plain [Column] of fixed 64 dp
 * rows (≤ 50 items, no new drag-and-drop dependency). Long-press-dragging a row's handle offsets it
 * by the finger's movement; the target index is the dragged row's index plus the rounded offset
 * divided by the row height. [onMove] fires once, on drop, if the target differs from the start;
 * [onDragStart]/[onDragEnd] let the caller play the DragStart/DragEnd haptics (MOTION §3). Neighbours
 * reflow instantly as the target index changes (MOT-12's reduced-motion end state, Q11-A): no lift
 * scale, no spring, only the dragged row's own `level4` container and shadow.
 */
@Composable
fun <K : Any> RepzExerciseReorderList(
    items: ImmutableList<RepzReorderItem<K>>,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    dragHandleContentDescription: String,
    modifier: Modifier = Modifier,
) {
    val rowHeightPx = with(LocalDensity.current) { ReorderRowHeight.toPx() }
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    val targetIndex =
        draggingIndex?.let { from -> (from + (dragOffsetPx / rowHeightPx).roundToInt()).coerceIn(0, items.lastIndex) }

    Column(modifier = modifier) {
        items.forEachIndexed { index, item ->
            val isDragged = index == draggingIndex
            val displacementRows = reorderDisplacementRows(index, draggingIndex, targetIndex)
            ReorderRow(
                item = item,
                isDragged = isDragged,
                dragHandleContentDescription = dragHandleContentDescription,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .zIndex(if (isDragged) 1f else 0f)
                        .graphicsLayer { translationY = if (isDragged) dragOffsetPx else displacementRows * rowHeightPx },
                onDragHandlePress = {
                    draggingIndex = index
                    dragOffsetPx = 0f
                    onDragStart()
                },
                onDrag = { deltaY -> dragOffsetPx += deltaY },
                onDragFinish = {
                    val from = draggingIndex
                    val to = targetIndex
                    draggingIndex = null
                    dragOffsetPx = 0f
                    if (from != null && to != null && from != to) onMove(from, to)
                    onDragEnd()
                },
            )
        }
    }
}

private fun reorderDisplacementRows(
    index: Int,
    draggingIndex: Int?,
    targetIndex: Int?,
): Float =
    when {
        draggingIndex == null || targetIndex == null -> 0f
        index == draggingIndex || draggingIndex == targetIndex -> 0f
        draggingIndex < targetIndex && index in (draggingIndex + 1)..targetIndex -> -1f
        draggingIndex > targetIndex && index in targetIndex until draggingIndex -> 1f
        else -> 0f
    }

@Composable
private fun <K : Any> ReorderRow(
    item: RepzReorderItem<K>,
    isDragged: Boolean,
    dragHandleContentDescription: String,
    onDragHandlePress: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.medium
    val elevation = if (isDragged) RepzElevation.level4 else null
    val containerColor = elevation?.containerColor ?: MaterialTheme.colorScheme.surfaceContainer
    val customActionList = reorderRowCustomActions(item)

    Row(
        modifier =
            modifier
                .height(ReorderRowHeight)
                .let { if (elevation?.shadow != null) it.dropShadow(shape, elevation.shadow) else it }
                .clip(shape)
                .background(containerColor)
                .padding(horizontal = RepzSpacing.lg)
                .semantics { customActions = customActionList },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RepzSpacing.md),
    ) {
        ReorderDragHandle(
            contentDescription = dragHandleContentDescription,
            onDragHandlePress = onDragHandlePress,
            onDrag = onDrag,
            onDragFinish = onDragFinish,
            modifier = Modifier.size(RepzIconSize.default),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(text = item.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (item.isDone) {
            Icon(
                painter = painterResource(Res.drawable.symbol_check_circle_filled),
                contentDescription = null,
                modifier = Modifier.size(RepzIconSize.default),
                tint = RepzTheme.colors.success,
            )
        }
    }
}

@Composable
private fun <K : Any> reorderRowCustomActions(item: RepzReorderItem<K>): List<CustomAccessibilityAction> =
    remember(item.moveUp, item.moveDown) {
        listOfNotNull(
            item.moveUp?.toCustomAccessibilityAction(),
            item.moveDown?.toCustomAccessibilityAction(),
        )
    }

private fun RepzAccessibilityAction.toCustomAccessibilityAction(): CustomAccessibilityAction =
    CustomAccessibilityAction(label) {
        onAction()
        true
    }

// The drag gesture's coroutine (pointerInput(Unit)) is started once and never restarted, so the
// callbacks it closes over must be read through rememberUpdatedState: draggingIndex/targetIndex
// (and so these lambdas) change on every drag delta in the parent.
@Composable
private fun ReorderDragHandle(
    contentDescription: String,
    onDragHandlePress: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnDragHandlePress by rememberUpdatedState(onDragHandlePress)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragFinish by rememberUpdatedState(onDragFinish)

    Icon(
        painter = painterResource(Res.drawable.symbol_drag_indicator),
        contentDescription = contentDescription,
        modifier =
            modifier.pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { currentOnDragHandlePress() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        currentOnDrag(dragAmount.y)
                    },
                    onDragEnd = { currentOnDragFinish() },
                    onDragCancel = { currentOnDragFinish() },
                )
            },
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
