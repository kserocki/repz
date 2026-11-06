package com.repz.app.core.designsystem.component.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.repz.app.core.designsystem.generated.resources.Res
import com.repz.app.core.designsystem.generated.resources.symbol_check
import com.repz.app.core.designsystem.generated.resources.symbol_delete
import com.repz.app.core.designsystem.theme.RepzIconSize
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.designsystem.theme.RepzTheme
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt

// DESIGN_SYSTEM §6/§9, board 18: row 56 dp (64 dp at fontScale > 1.3), 2-line reflow at
// fontScale ≥ 1.5 (≥ 72 dp). Component-internal: not on the §6 spacing scale.
private val DefaultRowHeight = 56.dp
private val TallRowHeight = 64.dp
private val ReflowRowMinHeight = 72.dp
private const val TALL_ROW_FONT_SCALE_THRESHOLD = 1.3f
private const val REFLOW_FONT_SCALE_THRESHOLD = 1.5f

// Columns: badge 38 · previous 68 · weight 1fr · reps 1fr · check 50; start padding 8, gap 6
// (board 18, component-internal exact board measurements, same precedent as elsewhere here).
internal val SetBadgeColumnWidth = 38.dp
internal val SetPreviousColumnWidth = 68.dp
internal val SetCheckColumnWidth = 50.dp
internal val SetRowStartPadding = 8.dp
internal val SetColumnGap = 6.dp

private val NarrowRowWidthThreshold = 360.dp

private val BadgeVisualSize = 28.dp
private val CheckboxTouchTarget = 44.dp
private val CheckboxVisualSize = 36.dp
private val CheckedCornerRadius = 10.dp
private val CheckboxBorderWidth = 2.5.dp
private val EditingRingWidth = 2.dp
private val FieldMinHeight = 44.dp

/**
 * One set (DESIGN_SYSTEM §9 `SetRow`, board 05): badge (tap opens [typeMenuItems] when non-empty,
 * Q3), optional Previous column (hidden below [NarrowRowWidthThreshold] or when reflowed), weight
 * and reps fields (tap opens the keypad on that field; [editingField] draws the 2 dp ring), and the
 * completion toggle. End-to-start swipe past 40 % of the row's width calls [onDelete] (MOT-12's
 * reduced-motion end state, Q11-A: the row follows the finger, then is removed instantly — no
 * commit animation here); a start-to-end drag is clamped to 0 and does nothing. At fontScale ≥ 1.5
 * the row reflows to two lines (badge + [lastLineText] on top, fields + check below, board 14).
 */
@Composable
fun RepzSetRow(
    badgeLabel: String,
    type: RepzSetType,
    previousText: String?,
    noPreviousText: String,
    lastLineText: String?,
    weightText: String,
    repsText: String,
    isPrefilled: Boolean,
    editingField: RepzSetField?,
    isCompleted: Boolean,
    typeMenuItems: ImmutableList<RepzMenuItem>,
    onWeightClick: () -> Unit,
    onRepsClick: () -> Unit,
    onCompleteClick: () -> Unit,
    onDelete: () -> Unit,
    accessibilityActions: ImmutableList<RepzAccessibilityAction>,
    completeContentDescription: String,
    weightContentDescription: String,
    repsContentDescription: String,
    badgeContentDescription: String,
    modifier: Modifier = Modifier,
) {
    val fontScale = LocalDensity.current.fontScale
    val isReflowed = fontScale >= REFLOW_FONT_SCALE_THRESHOLD
    val containerColor = if (isCompleted) RepzTheme.colors.successContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    val customActionList = rememberSetRowCustomActions(accessibilityActions)

    SwipeToDeleteBox(
        onDelete = onDelete,
        containerColor = containerColor,
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = setRowMinHeight(fontScale))
                .semantics { customActions = customActionList },
    ) {
        if (isReflowed) {
            SetRowReflowed(
                badgeLabel = badgeLabel,
                type = type,
                lastLineText = lastLineText,
                weightText = weightText,
                repsText = repsText,
                isPrefilled = isPrefilled,
                editingField = editingField,
                isCompleted = isCompleted,
                typeMenuItems = typeMenuItems,
                onWeightClick = onWeightClick,
                onRepsClick = onRepsClick,
                onCompleteClick = onCompleteClick,
                completeContentDescription = completeContentDescription,
                weightContentDescription = weightContentDescription,
                repsContentDescription = repsContentDescription,
                badgeContentDescription = badgeContentDescription,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            SetRowSingleLine(
                badgeLabel = badgeLabel,
                type = type,
                previousText = previousText,
                noPreviousText = noPreviousText,
                weightText = weightText,
                repsText = repsText,
                isPrefilled = isPrefilled,
                editingField = editingField,
                isCompleted = isCompleted,
                typeMenuItems = typeMenuItems,
                onWeightClick = onWeightClick,
                onRepsClick = onRepsClick,
                onCompleteClick = onCompleteClick,
                completeContentDescription = completeContentDescription,
                weightContentDescription = weightContentDescription,
                repsContentDescription = repsContentDescription,
                badgeContentDescription = badgeContentDescription,
                isPreviousColumnVisible = maxWidth >= NarrowRowWidthThreshold,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun rememberSetRowCustomActions(actions: ImmutableList<RepzAccessibilityAction>): List<CustomAccessibilityAction> =
    remember(actions) {
        actions.map { action ->
            CustomAccessibilityAction(action.label) {
                action.onAction()
                true
            }
        }
    }

private fun setRowMinHeight(fontScale: Float): Dp =
    when {
        fontScale >= REFLOW_FONT_SCALE_THRESHOLD -> ReflowRowMinHeight
        fontScale > TALL_ROW_FONT_SCALE_THRESHOLD -> TallRowHeight
        else -> DefaultRowHeight
    }

@Suppress("LongParameterList")
@Composable
private fun SetRowSingleLine(
    badgeLabel: String,
    type: RepzSetType,
    previousText: String?,
    noPreviousText: String,
    weightText: String,
    repsText: String,
    isPrefilled: Boolean,
    editingField: RepzSetField?,
    isCompleted: Boolean,
    typeMenuItems: ImmutableList<RepzMenuItem>,
    onWeightClick: () -> Unit,
    onRepsClick: () -> Unit,
    onCompleteClick: () -> Unit,
    completeContentDescription: String,
    weightContentDescription: String,
    repsContentDescription: String,
    badgeContentDescription: String,
    isPreviousColumnVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(start = SetRowStartPadding, end = RepzSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SetColumnGap),
    ) {
        SetBadge(
            label = badgeLabel,
            type = type,
            isCompleted = isCompleted,
            menuItems = typeMenuItems,
            contentDescription = badgeContentDescription,
            modifier = Modifier.width(SetBadgeColumnWidth),
        )
        if (isPreviousColumnVisible) {
            Text(
                text = previousText ?: noPreviousText,
                modifier = Modifier.width(SetPreviousColumnWidth),
                style = RepzTheme.typography.numericSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SetValueField(
            text = weightText,
            isPrefilled = isPrefilled,
            isCompleted = isCompleted,
            isEditing = editingField == RepzSetField.Weight,
            onClick = onWeightClick,
            contentDescription = weightContentDescription,
            modifier = Modifier.weight(1f).heightIn(min = FieldMinHeight),
        )
        SetValueField(
            text = repsText,
            isPrefilled = isPrefilled,
            isCompleted = isCompleted,
            isEditing = editingField == RepzSetField.Reps,
            onClick = onRepsClick,
            contentDescription = repsContentDescription,
            modifier = Modifier.weight(1f).heightIn(min = FieldMinHeight),
        )
        SetCompleteCheckbox(
            isCompleted = isCompleted,
            onClick = onCompleteClick,
            contentDescription = completeContentDescription,
            modifier = Modifier.width(SetCheckColumnWidth),
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun SetRowReflowed(
    badgeLabel: String,
    type: RepzSetType,
    lastLineText: String?,
    weightText: String,
    repsText: String,
    isPrefilled: Boolean,
    editingField: RepzSetField?,
    isCompleted: Boolean,
    typeMenuItems: ImmutableList<RepzMenuItem>,
    onWeightClick: () -> Unit,
    onRepsClick: () -> Unit,
    onCompleteClick: () -> Unit,
    completeContentDescription: String,
    weightContentDescription: String,
    repsContentDescription: String,
    badgeContentDescription: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = SetRowStartPadding, vertical = RepzSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(RepzSpacing.xxs),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(SetColumnGap), verticalAlignment = Alignment.CenterVertically) {
            SetBadge(
                label = badgeLabel,
                type = type,
                isCompleted = isCompleted,
                menuItems = typeMenuItems,
                contentDescription = badgeContentDescription,
                modifier = Modifier.width(SetBadgeColumnWidth),
            )
            if (lastLineText != null) {
                Text(lastLineText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(SetColumnGap), verticalAlignment = Alignment.CenterVertically) {
            SetValueField(
                text = weightText,
                isPrefilled = isPrefilled,
                isCompleted = isCompleted,
                isEditing = editingField == RepzSetField.Weight,
                onClick = onWeightClick,
                contentDescription = weightContentDescription,
                modifier = Modifier.weight(1f).heightIn(min = FieldMinHeight),
            )
            SetValueField(
                text = repsText,
                isPrefilled = isPrefilled,
                isCompleted = isCompleted,
                isEditing = editingField == RepzSetField.Reps,
                onClick = onRepsClick,
                contentDescription = repsContentDescription,
                modifier = Modifier.weight(1f).heightIn(min = FieldMinHeight),
            )
            SetCompleteCheckbox(
                isCompleted = isCompleted,
                onClick = onCompleteClick,
                contentDescription = completeContentDescription,
                modifier = Modifier.width(SetCheckColumnWidth),
            )
        }
    }
}

@Composable
private fun SetBadge(
    label: String,
    type: RepzSetType,
    isCompleted: Boolean,
    menuItems: ImmutableList<RepzMenuItem>,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(false) }
    val isClickable = menuItems.isNotEmpty()

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier =
                Modifier
                    .size(BadgeVisualSize)
                    .clip(CircleShape)
                    .background(type.badgeContainerColor(isCompleted))
                    .then(if (isClickable) Modifier.clickable { isExpanded = true } else Modifier)
                    .semantics { this.contentDescription = contentDescription },
            contentAlignment = Alignment.Center,
        ) {
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = type.badgeContentColor(isCompleted))
        }
        if (isClickable) {
            RepzWorkoutDropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }, items = menuItems)
        }
    }
}

// A completed Normal badge pops in the stronger `success`/`onSuccess` pair against the row's
// lighter `successContainer` background; W/D/F badges keep their own type colour either way
// (board 05/09: a completed warm-up set's badge stays the warm-up colour, never turns green).
@Composable
private fun RepzSetType.badgeContainerColor(isCompleted: Boolean): Color =
    when {
        this == RepzSetType.Normal && isCompleted -> RepzTheme.colors.success
        this == RepzSetType.Warmup -> RepzTheme.colors.setWarmupContainer
        this == RepzSetType.Drop -> RepzTheme.colors.setDropContainer
        this == RepzSetType.Failure -> RepzTheme.colors.setFailureContainer
        else -> RepzTheme.colors.setNormalContainer
    }

@Composable
private fun RepzSetType.badgeContentColor(isCompleted: Boolean): Color =
    when {
        this == RepzSetType.Normal && isCompleted -> RepzTheme.colors.onSuccess
        this == RepzSetType.Warmup -> RepzTheme.colors.onSetWarmupContainer
        this == RepzSetType.Drop -> RepzTheme.colors.onSetDropContainer
        this == RepzSetType.Failure -> RepzTheme.colors.onSetFailureContainer
        else -> RepzTheme.colors.onSetNormalContainer
    }

@Composable
private fun SetValueField(
    text: String,
    isPrefilled: Boolean,
    isCompleted: Boolean,
    isEditing: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.small
    val backgroundColor = if (isCompleted) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHighest
    val textColor =
        when {
            isCompleted -> RepzTheme.colors.onSuccessContainer
            isPrefilled -> MaterialTheme.colorScheme.onSurfaceVariant
            else -> MaterialTheme.colorScheme.onSurface
        }

    Box(
        modifier =
            modifier
                .clip(shape)
                .background(backgroundColor)
                .then(if (isEditing) Modifier.border(EditingRingWidth, MaterialTheme.colorScheme.primary, shape) else Modifier)
                .clickable(onClick = onClick)
                .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = RepzTheme.typography.numericLarge, color = textColor)
    }
}

@Composable
private fun SetCompleteCheckbox(
    isCompleted: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val shape: Shape = if (isCompleted) RoundedCornerShape(CheckedCornerRadius) else CircleShape
    Box(
        modifier =
            modifier
                .size(CheckboxTouchTarget)
                .clickable(onClick = onClick)
                .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(CheckboxVisualSize)
                    .clip(shape)
                    .background(if (isCompleted) RepzTheme.colors.success else Color.Transparent)
                    .then(
                        if (isCompleted) {
                            Modifier
                        } else {
                            Modifier.border(CheckboxBorderWidth, MaterialTheme.colorScheme.outline, shape)
                        },
                    ),
            contentAlignment = Alignment.Center,
        ) {
            if (isCompleted) {
                Icon(
                    painter = painterResource(Res.drawable.symbol_check),
                    contentDescription = null,
                    modifier = Modifier.size(RepzIconSize.dense),
                    tint = RepzTheme.colors.onSuccess,
                )
            }
        }
    }
}
