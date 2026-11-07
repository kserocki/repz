package com.repz.app.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.repz.app.core.designsystem.motion.LocalReducedMotion
import com.repz.app.core.designsystem.motion.RepzMotion
import com.repz.app.core.designsystem.theme.RepzSpacing

// Board 04 "Finish ▾" split button (component-internal, exact). Internal (not private) so
// RepzHeroStartButtonSpecTest can assert the shared U2(g) corner table against this variant too.
internal val SplitButtonHeight = 40.dp
private val SplitButtonGap = 2.dp
internal val SplitButtonInnerRadius = 6.dp
internal val SplitButtonPressedOuterRadius = 12.dp
private val TrailingSegmentWidth = 44.dp
private val TrailingIconSize = 20.dp

private const val DISABLED_CONTAINER_ALPHA = 0.12f
private const val DISABLED_CONTENT_ALPHA = 0.38f
private const val PRESSED_SEGMENT_SCALE = 0.96f

/**
 * A primary action with a secondary menu trigger (DESIGN_SYSTEM §9, board 04 "Finish ▾"): a 40 dp
 * tall, `Filled` (`primary`/`onPrimary`) pair of segments, [SplitButtonGap] apart, outer corners
 * full at rest and [SplitButtonInnerRadius] (6 dp) between them. [onClick] fires the leading
 * segment's main action ([text], `labelLarge`); [onTrailingClick] fires the trailing 44 dp segment
 * (`KeyboardArrowDown`), which opens the caller's own menu. [text] always wraps, never truncates.
 *
 * MOT-22/U2(g): only the pressed segment's outer corners morph to [SplitButtonPressedOuterRadius]
 * and it scales to 0.96; the shared inner corners never move. Reduced motion: state layer only.
 *
 * The row measures its height from `IntrinsicSize.Min` and each segment declares its own
 * `heightIn(min = SplitButtonHeight)`, so a wrapped label (PL or a 200 % font scale) grows the
 * button instead of being clipped by a fixed height (required change 3, AGENTS.md §4 "buttons wrap,
 * never truncate").
 */
@Composable
fun RepzSplitButton(
    text: String,
    onClick: () -> Unit,
    onTrailingClick: () -> Unit,
    trailingContentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val leadingInteractionSource = remember { MutableInteractionSource() }
    val trailingInteractionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier.height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(SplitButtonGap),
    ) {
        RepzSplitButtonSegment(
            interactionSource = leadingInteractionSource,
            outerOnStart = true,
            enabled = enabled,
            onClick = onClick,
            modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = SplitButtonHeight),
        ) {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }

        RepzSplitButtonSegment(
            interactionSource = trailingInteractionSource,
            outerOnStart = false,
            enabled = enabled,
            onClick = onTrailingClick,
            modifier = Modifier.width(TrailingSegmentWidth).fillMaxHeight().heightIn(min = SplitButtonHeight),
            contentDescription = trailingContentDescription,
        ) {
            RepzIcon(RepzIcon.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(TrailingIconSize))
        }
    }
}

@Composable
private fun RepzSplitButtonSegment(
    interactionSource: MutableInteractionSource,
    outerOnStart: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    content: @Composable () -> Unit,
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
            isFirst = outerOnStart,
            isLast = !outerOnStart,
            outerRadius = PressMorphCornerSize(SplitButtonPressedOuterRadius, pressFraction),
            innerRadius = CornerSize(SplitButtonInnerRadius),
        )
    val shape = RoundedCornerShape(corners.topStart, corners.topEnd, corners.bottomEnd, corners.bottomStart)

    val containerColor: Color
    val contentColor: Color
    if (enabled) {
        containerColor = MaterialTheme.colorScheme.primary
        contentColor = MaterialTheme.colorScheme.onPrimary
    } else {
        containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_CONTAINER_ALPHA)
        contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_CONTENT_ALPHA)
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        interactionSource = interactionSource,
        modifier =
            modifier.scale(scale).semantics {
                role = Role.Button
                if (contentDescription != null) this.contentDescription = contentDescription
            },
    ) {
        Box(modifier = Modifier.padding(horizontal = RepzSpacing.md), contentAlignment = Alignment.Center) {
            content()
        }
    }
}
