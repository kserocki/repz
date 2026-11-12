package com.repz.app.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.repz.app.core.designsystem.motion.LocalReducedMotion
import com.repz.app.core.designsystem.motion.RepzMotion
import com.repz.app.core.designsystem.theme.RepzSpacing
import kotlinx.collections.immutable.ImmutableList

// Board 04 "kg lb" connected group (component-internal, exact): height 40, 2 dp gap, inner corners
// 6, 18 dp check icon. Pressed outer radius matches RepzButton's shared 12 dp (MOT-22).
private val GroupHeight = 40.dp
private val GroupGap = 2.dp
private val InnerCornerRadius = 6.dp
private val PressedOuterCornerRadius = 12.dp
private val CheckIconSize = 18.dp

private const val DISABLED_CONTAINER_ALPHA = 0.12f
private const val DISABLED_CONTENT_ALPHA = 0.38f
private const val PRESSED_SEGMENT_SCALE = 0.96f

/** One option's 4 corner radii (top-start, top-end, bottom-end, bottom-start), as a plain value
 * class so the first/last/middle → outer/inner mapping is testable without a composition. Generic
 * so the same mapping serves both a precomputed [Dp] (tests) and a measured-size-aware [CornerSize]
 * (actual shapes). */
internal data class RepzSegmentCornerRadii<T>(
    val topStart: T,
    val topEnd: T,
    val bottomEnd: T,
    val bottomStart: T,
)

/**
 * U2(g)'s rule generalised to N options: only an option's *outer* corners (the ones with no
 * neighbour on that side) ever show [outerRadius]; every corner shared with a neighbour stays at
 * [innerRadius], pressed or not. A single-option group (`isFirst && isLast`) is outer on both sides.
 */
internal fun <T> connectedGroupSegmentCorners(
    isFirst: Boolean,
    isLast: Boolean,
    outerRadius: T,
    innerRadius: T,
): RepzSegmentCornerRadii<T> {
    val startRadius = if (isFirst) outerRadius else innerRadius
    val endRadius = if (isLast) outerRadius else innerRadius
    return RepzSegmentCornerRadii(topStart = startRadius, topEnd = endRadius, bottomEnd = endRadius, bottomStart = startRadius)
}

/**
 * A single-select segmented control (DESIGN_SYSTEM §9 "connected button group", board 04 "kg / lb"):
 * [GroupHeight] tall, [GroupGap] apart, outer corners full at rest and [InnerCornerRadius] between
 * options. The selected [options] entry shows [CheckIconSize] leading `Check` icon on
 * `primary`/`onPrimary`; others are `surfaceContainerHighest`/`onSurfaceVariant`. [options] always
 * wraps, never truncates. Announced as a radio group (`selectableGroup()` + `Role.RadioButton` per
 * option), so accessibility services read it the same way as a native radio button row.
 *
 * MOT-22: only the pressed option's outer corners morph to [PressedOuterCornerRadius] and it scales
 * to 0.96; its inner (shared) corners never move. Reduced motion: state layer only.
 *
 * The row measures its height from `IntrinsicSize.Min` and each option declares its own
 * `heightIn(min = GroupHeight)`, so every option shares the tallest option's height even under an
 * unbounded-height parent (a scrolling column/`LazyColumn`) — a plain `heightIn(min = GroupHeight)`
 * on the row alone leaves `fillMaxHeight()` a no-op there.
 */
@Composable
fun RepzConnectedButtonGroup(
    options: ImmutableList<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSources = remember(options.size) { List(options.size) { MutableInteractionSource() } }

    Row(
        modifier = modifier.height(IntrinsicSize.Min).width(IntrinsicSize.Min).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(GroupGap),
    ) {
        options.forEachIndexed { index, option ->
            RepzConnectedButtonGroupOption(
                label = option,
                selected = index == selectedIndex,
                position = RepzConnectedGroupPosition(isFirst = index == 0, isLast = index == options.lastIndex),
                enabled = enabled,
                interactionSource = interactionSources[index],
                onClick = { onSelectedIndexChange(index) },
                modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = GroupHeight),
            )
        }
    }
}

/** First/last bundled into one parameter so [RepzConnectedButtonGroupOption] stays within Detekt's
 * `LongParameterList` threshold. */
private data class RepzConnectedGroupPosition(
    val isFirst: Boolean,
    val isLast: Boolean,
)

@Composable
private fun RepzConnectedButtonGroupOption(
    label: String,
    selected: Boolean,
    position: RepzConnectedGroupPosition,
    enabled: Boolean,
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    val morphed = isPressed && !LocalReducedMotion.current

    val pressFraction by
        animateFloatAsState(
            targetValue = if (morphed) 1f else 0f,
            animationSpec = RepzMotion.springs.pressMorph(),
        )
    val scale by
        animateFloatAsState(
            targetValue = if (morphed) PRESSED_SEGMENT_SCALE else 1f,
            animationSpec = RepzMotion.springs.pressMorph(),
        )

    val corners =
        connectedGroupSegmentCorners(
            isFirst = position.isFirst,
            isLast = position.isLast,
            outerRadius = PressMorphCornerSize(PressedOuterCornerRadius, pressFraction),
            innerRadius = CornerSize(InnerCornerRadius),
        )
    val shape = RoundedCornerShape(corners.topStart, corners.topEnd, corners.bottomEnd, corners.bottomStart)
    val colors = connectedGroupOptionColors(enabled, selected)

    Surface(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        color = colors.container,
        contentColor = colors.content,
        interactionSource = interactionSource,
        modifier = modifier.scale(scale).semantics { role = Role.RadioButton },
    ) {
        RepzConnectedButtonGroupOptionBody(label, selected)
    }
}

private data class RepzConnectedGroupOptionColors(
    val container: Color,
    val content: Color,
)

@Composable
private fun connectedGroupOptionColors(
    enabled: Boolean,
    selected: Boolean,
): RepzConnectedGroupOptionColors =
    when {
        !enabled -> {
            RepzConnectedGroupOptionColors(
                container = MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_CONTAINER_ALPHA),
                content = MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_CONTENT_ALPHA),
            )
        }

        selected -> {
            RepzConnectedGroupOptionColors(container = MaterialTheme.colorScheme.primary, content = MaterialTheme.colorScheme.onPrimary)
        }

        else -> {
            RepzConnectedGroupOptionColors(
                container = MaterialTheme.colorScheme.surfaceContainerHighest,
                content = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

@Composable
private fun RepzConnectedButtonGroupOptionBody(
    label: String,
    selected: Boolean,
) {
    Row(
        modifier = Modifier.padding(horizontal = RepzSpacing.md),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            RepzIcon(RepzIcon.Check, contentDescription = null, modifier = Modifier.size(CheckIconSize))
            Spacer(Modifier.width(RepzSpacing.xs))
        }
        Text(label, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}
