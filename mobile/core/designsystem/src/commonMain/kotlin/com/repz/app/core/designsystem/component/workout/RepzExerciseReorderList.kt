package com.repz.app.core.designsystem.component.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.layout.onSizeChanged
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

// DESIGN_SYSTEM §9, board 09 §10b: reorder rows collapse to 64 dp; a minimum, not a fixed height,
// so a row's title/subtitle can still wrap at a large font scale instead of clipping (board 14).
private val ReorderRowMinHeight = 64.dp

// DESIGN_SYSTEM §6's 48 dp minimum touch target: bigger than the 24 dp drag_indicator glyph itself.
private val DragHandleTouchTarget = 48.dp

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
 * The reorder-mode list (DESIGN_SYSTEM §9, board 09 §10b): a plain [Column] of rows (≤ 50 items),
 * built without pulling in a drag-and-drop library for this single use. Long-press-dragging a row's
 * handle offsets it by the finger's movement; the target index is found by walking each row's own
 * *measured* height outward from the dragged row's start index (not a fixed row height, since a
 * large font scale makes every row taller, board 14). [onMove] fires once, on drop, if the target
 * differs from the start — never on a cancelled drag (the system interrupting the gesture, e.g. a
 * parent claiming the touch, resets the row without moving anything). [onDragStart]/[onDragEnd] let
 * the caller play the DragStart/DragEnd haptics (MOTION §3) on every drop, cancelled or not.
 * Neighbours reflow instantly as the target index changes (this ships only MOT-12's documented
 * reduced-motion end state, since this issue adds no MOT-xx animation): no lift scale, no spring,
 * only the dragged row's own `level4` container and shadow.
 *
 * Give every item a [RepzReorderItem.key] that's stable and distinct from every other item's: this
 * list wraps each row in `key(item.key) { … }`, so Compose disposes (rather than reuses by position)
 * the composable — and its remembered drag state — for a row that's actually been removed or moved
 * elsewhere in the list.
 *
 * TODO(REP-46): replace the instant reflow with MOT-12's lift scale, shadow and spring.
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
    val fallbackRowHeightPx = with(LocalDensity.current) { ReorderRowMinHeight.toPx() }
    val rowHeightsPx = remember { mutableStateMapOf<Int, Int>() }
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    val targetIndexState =
        remember {
            derivedStateOf {
                draggingIndex?.let { from ->
                    targetIndexFor(from, dragOffsetPx, rowHeightsPx, fallbackRowHeightPx, items.lastIndex)
                }
            }
        }
    val targetIndex by targetIndexState

    Column(modifier = modifier) {
        items.forEachIndexed { index, item ->
            key(item.key) {
                val isDragged = index == draggingIndex
                val rowHeightPx = rowHeightsPx[index] ?: fallbackRowHeightPx.roundToInt()
                val displacementPx = reorderDisplacementRows(index, draggingIndex, targetIndex) * rowHeightPx
                ReorderRow(
                    item = item,
                    isDragged = isDragged,
                    dragHandleContentDescription = dragHandleContentDescription,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .onSizeChanged { size -> rowHeightsPx[index] = size.height }
                            .zIndex(if (isDragged) 1f else 0f)
                            .graphicsLayer { translationY = if (isDragged) dragOffsetPx else displacementPx },
                    onDragHandlePress = {
                        draggingIndex = index
                        dragOffsetPx = 0f
                        onDragStart()
                    },
                    onDrag = { deltaY -> dragOffsetPx += deltaY },
                    onDragFinish = { wasCancelled ->
                        val from = draggingIndex
                        // Read fresh here rather than relying on the `targetIndex` captured when this
                        // lambda was built: rememberUpdatedState only guarantees the latest *lambda*,
                        // not that a recomposition carrying the latest target already ran first.
                        val to = targetIndexState.value
                        draggingIndex = null
                        dragOffsetPx = 0f
                        if (!wasCancelled && from != null && to != null) {
                            if (from != to) onMove(from, to)
                        }
                        onDragEnd()
                    },
                )
            }
        }
    }
}

private fun targetIndexFor(
    draggingIndex: Int,
    dragOffsetPx: Float,
    rowHeightsPx: Map<Int, Int>,
    fallbackHeightPx: Float,
    lastIndex: Int,
): Int {
    var remaining = dragOffsetPx
    var index = draggingIndex
    while (remaining > 0f && index < lastIndex) {
        val nextHeight = (rowHeightsPx[index + 1] ?: fallbackHeightPx.roundToInt()).toFloat()
        if (remaining < nextHeight / 2f) break
        remaining -= nextHeight
        index++
    }
    while (remaining < 0f && index > 0) {
        val previousHeight = (rowHeightsPx[index - 1] ?: fallbackHeightPx.roundToInt()).toFloat()
        if (-remaining < previousHeight / 2f) break
        remaining += previousHeight
        index--
    }
    return index
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
    onDragFinish: (wasCancelled: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.medium
    val elevation = if (isDragged) RepzElevation.level4 else null
    val containerColor = elevation?.containerColor ?: MaterialTheme.colorScheme.surfaceContainer
    val customActionList = reorderRowCustomActions(item)

    Row(
        modifier =
            modifier
                .heightIn(min = ReorderRowMinHeight)
                .let { if (elevation?.shadow != null) it.dropShadow(shape, elevation.shadow) else it }
                .clip(shape)
                .background(containerColor)
                .padding(horizontal = RepzSpacing.lg)
                // Merged into one accessible node (title + subtitle become its label) so the custom
                // Move up/down actions are reachable regardless of which child last had focus (M2).
                .semantics(mergeDescendants = true) { customActions = customActionList },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RepzSpacing.md),
    ) {
        ReorderDragHandle(
            contentDescription = dragHandleContentDescription,
            onDragHandlePress = onDragHandlePress,
            onDrag = onDrag,
            onDragFinish = onDragFinish,
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
    onDragFinish: (wasCancelled: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnDragHandlePress by rememberUpdatedState(onDragHandlePress)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragFinish by rememberUpdatedState(onDragFinish)

    Box(
        modifier =
            modifier
                .size(DragHandleTouchTarget)
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { currentOnDragHandlePress() },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            currentOnDrag(dragAmount.y)
                        },
                        onDragEnd = { currentOnDragFinish(false) },
                        onDragCancel = { currentOnDragFinish(true) },
                    )
                },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(Res.drawable.symbol_drag_indicator),
            contentDescription = contentDescription,
            modifier = Modifier.size(RepzIconSize.default),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
