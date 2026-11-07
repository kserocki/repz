package com.repz.app.core.designsystem.keypad

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.repz.app.core.designsystem.generated.resources.Res
import com.repz.app.core.designsystem.generated.resources.keypad_mode_reps
import com.repz.app.core.designsystem.generated.resources.keypad_mode_reps_short
import com.repz.app.core.designsystem.generated.resources.keypad_mode_rpe
import com.repz.app.core.designsystem.generated.resources.keypad_mode_rpe_short
import com.repz.app.core.designsystem.generated.resources.keypad_mode_weight
import com.repz.app.core.designsystem.generated.resources.keypad_mode_weight_short
import com.repz.app.core.designsystem.theme.RepzSpacing
import org.jetbrains.compose.resources.stringResource

private val ModeSegmentHeight = 36.dp
private val ModeSegmentTouchTarget = 48.dp
private val ModeSegmentEndRadius = ModeSegmentHeight / 2
private val ModeSegmentGap = RepzSpacing.xxs

@Composable
private fun KeypadMode.label(): String =
    when (this) {
        KeypadMode.Weight -> stringResource(Res.string.keypad_mode_weight)
        KeypadMode.Reps -> stringResource(Res.string.keypad_mode_reps)
        KeypadMode.Rpe -> stringResource(Res.string.keypad_mode_rpe)
    }

/** Q1 (review-1): shown instead of the full label when that doesn't fit the segment on one line
 * (PL's "Powtórzenia"); EN/RPE are already short, so their value equals [label]. */
@Composable
private fun KeypadMode.shortLabel(): String =
    when (this) {
        KeypadMode.Weight -> stringResource(Res.string.keypad_mode_weight_short)
        KeypadMode.Reps -> stringResource(Res.string.keypad_mode_reps_short)
        KeypadMode.Rpe -> stringResource(Res.string.keypad_mode_rpe_short)
    }

/**
 * The Weight/Reps/[RPE] segmented control (DESIGN_SYSTEM §9); hidden when there is only one mode.
 * Every segment shares one [IntrinsicSize.Min] height (review-1 F4): a segment whose label wraps
 * (at 200% font) grows the whole row instead of clipping. The outer [BoxWithConstraints] computes
 * each segment's width once, outside that intrinsic-height row, so [ModeSegmentLabel]'s own width
 * measurement (`BoxWithConstraints`/`SubcomposeLayout`, which doesn't support intrinsic queries)
 * never sits inside a subtree the row needs to query intrinsically.
 */
@Composable
internal fun KeypadModeRow(
    modes: List<KeypadMode>,
    selectedMode: KeypadMode,
    onModeClick: (KeypadMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (modes.size <= 1) return

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val segmentWidth = (maxWidth - ModeSegmentGap * (modes.size - 1)) / modes.size
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(ModeSegmentGap),
        ) {
            for ((index, mode) in modes.withIndex()) {
                val isSelected = mode == selectedMode
                val shape =
                    RoundedCornerShape(
                        topStart = if (index == 0) ModeSegmentEndRadius else 0.dp,
                        bottomStart = if (index == 0) ModeSegmentEndRadius else 0.dp,
                        topEnd = if (index == modes.lastIndex) ModeSegmentEndRadius else 0.dp,
                        bottomEnd = if (index == modes.lastIndex) ModeSegmentEndRadius else 0.dp,
                    )
                val segmentContainerColor =
                    if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest
                Box(
                    modifier = Modifier.width(segmentWidth).heightIn(min = ModeSegmentTouchTarget).fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .heightIn(min = ModeSegmentHeight)
                                .clip(shape)
                                .background(segmentContainerColor)
                                .clickable(role = Role.Button, onClick = { onModeClick(mode) })
                                .semantics { selected = isSelected },
                        contentAlignment = Alignment.Center,
                    ) {
                        ModeSegmentLabel(
                            fullLabel = mode.label(),
                            shortLabel = mode.shortLabel(),
                            availableWidth = segmentWidth,
                            color =
                                if (isSelected) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                    }
                }
            }
        }
    }
}

/**
 * A mode segment's label (Q1, review-1): measures [fullLabel] at `labelLarge` on one line
 * (`softWrap = false`); if that's wider than [availableWidth], shows [shortLabel] instead, so a
 * single word is never split mid-word (AGENTS §4). The accessible name is always [fullLabel].
 */
@Composable
private fun ModeSegmentLabel(
    fullLabel: String,
    shortLabel: String,
    availableWidth: Dp,
    color: Color,
) {
    val style = MaterialTheme.typography.labelLarge
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val label =
        remember(fullLabel, shortLabel, availableWidth, style, density) {
            val fullLabelWidthPx = textMeasurer.measure(text = fullLabel, style = style, softWrap = false).size.width
            val availableWidthPx = with(density) { availableWidth.roundToPx() }
            if (fullLabelWidthPx > availableWidthPx) shortLabel else fullLabel
        }
    Text(
        text = label,
        style = style,
        color = color,
        maxLines = 1,
        modifier = Modifier.semantics { contentDescription = fullLabel },
    )
}
