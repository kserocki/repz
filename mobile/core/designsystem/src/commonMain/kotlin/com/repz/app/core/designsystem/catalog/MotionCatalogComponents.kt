package com.repz.app.core.designsystem.catalog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import com.repz.app.core.designsystem.motion.LocalReducedMotion
import com.repz.app.core.designsystem.motion.RepzMorphShape
import com.repz.app.core.designsystem.motion.RepzStagger
import com.repz.app.core.designsystem.motion.rememberSystemReducedMotion
import com.repz.app.core.designsystem.theme.RepzSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val AUTO_PLAY_INTERVAL_MILLIS = 1500L
private val DotTrackWidth = 160.dp
private val DotSize = 16.dp
private val SwatchSize = 40.dp
private val FadeBoxSize = 40.dp
private const val FADE_ROW_DIMMED_ALPHA = 0.25f
private val DrawCanvasSize = 40.dp
private val DrawStrokeWidth = 4.dp
private val StaggerChipSize = 20.dp

/** Flips every [intervalMillis] so a catalog demo can auto-play, per F2's "motion demos auto-play
 * in a loop" (simctl can't tap). A tap on the row flips it the same way, for manual inspection on
 * Android. */
@Composable
internal fun rememberAutoPlayState(intervalMillis: Long = AUTO_PLAY_INTERVAL_MILLIS): MutableState<Boolean> {
    val target = remember { mutableStateOf(false) }
    LaunchedEffect(intervalMillis) {
        while (isActive) {
            delay(intervalMillis)
            target.value = !target.value
        }
    }
    return target
}

/**
 * `reduced motion: on|off (system|forced)`: reads the effective [LocalReducedMotion] plus the live
 * platform value independently, so the catalog can tell "forced" (the debug hook, or a
 * `RepzTheme(reducedMotion = …)` preview) from "system" without `RepzTheme` threading an extra
 * parameter down to this page.
 */
@Composable
internal fun ReducedMotionHeader(modifier: Modifier = Modifier) {
    val effective = LocalReducedMotion.current
    val system = rememberSystemReducedMotion()
    val source = if (effective == system) "system" else "forced"
    val onOff = if (effective) "on" else "off"
    CatalogSectionLabel("reduced motion: $onOff ($source)", modifier)
}

/** A dot travelling along a short track, driven by [animationSpec]: the springs-1/2 pages' way of
 * showing a spring's feel (speed, overshoot) without a real component. */
@Composable
internal fun SpringDotRow(
    label: String,
    animationSpec: FiniteAnimationSpec<Dp>,
    modifier: Modifier = Modifier,
) {
    val target = rememberAutoPlayState()
    val offset by animateDpAsState(targetValue = if (target.value) DotTrackWidth - DotSize else 0.dp, animationSpec = animationSpec)

    Column(
        modifier = modifier.clickable { target.value = !target.value },
        verticalArrangement = Arrangement.spacedBy(RepzSpacing.xxs),
    ) {
        CatalogSectionLabel(label)
        Box(
            modifier =
                Modifier
                    .width(DotTrackWidth)
                    .height(DotSize)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
        ) {
            Box(
                modifier =
                    Modifier
                        .offset(x = offset)
                        .size(DotSize)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
            )
        }
    }
}

/** A swatch cross-fading between two container colours, driven by [animationSpec]: the effects
 * springs' demo (colour/alpha only, never shape or position). */
@Composable
internal fun SwatchFadeRow(
    label: String,
    animationSpec: FiniteAnimationSpec<Color>,
    modifier: Modifier = Modifier,
) {
    val target = rememberAutoPlayState()
    val color by
        animateColorAsState(
            targetValue =
                if (target.value) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
            animationSpec = animationSpec,
        )

    Column(
        modifier = modifier.clickable { target.value = !target.value },
        verticalArrangement = Arrangement.spacedBy(RepzSpacing.xxs),
    ) {
        CatalogSectionLabel(label)
        Box(modifier = Modifier.size(SwatchSize).background(color, MaterialTheme.shapes.small))
    }
}

/** A box fading between dimmed and full alpha, driven by [animationSpec]: the `tweens.short`/
 * `medium` demo. */
@Composable
internal fun FadeRow(
    label: String,
    animationSpec: FiniteAnimationSpec<Float>,
    modifier: Modifier = Modifier,
) {
    val target = rememberAutoPlayState()
    val alpha by animateFloatAsState(targetValue = if (target.value) 1f else FADE_ROW_DIMMED_ALPHA, animationSpec = animationSpec)

    Column(
        modifier = modifier.clickable { target.value = !target.value },
        verticalArrangement = Arrangement.spacedBy(RepzSpacing.xxs),
    ) {
        CatalogSectionLabel(label)
        Box(
            modifier =
                Modifier
                    .size(FadeBoxSize)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha), MaterialTheme.shapes.small),
        )
    }
}

/** A unit-space (0..1) checkmark, scaled to the canvas size before each draw. */
private val CheckPathUnit =
    Path().apply {
        moveTo(0.2f, 0.55f)
        lineTo(0.42f, 0.75f)
        lineTo(0.8f, 0.3f)
    }

/** A unit-space zig-zag "chart" line, scaled to the canvas size before each draw. */
private val ChartPathUnit =
    Path().apply {
        moveTo(0f, 0.8f)
        lineTo(0.25f, 0.5f)
        lineTo(0.5f, 0.65f)
        lineTo(0.75f, 0.2f)
        lineTo(1f, 0.1f)
    }

/** Draws the leading [progress] fraction of [unitPath] (scaled to the draw size), per MOTION.md
 * §5's "Path draw" implementation note: `PathMeasure.getSegment` driven by a tween. */
private fun DrawScope.drawPathProgress(
    unitPath: Path,
    progress: Float,
    color: Color,
) {
    val scaled =
        Path().apply {
            addPath(unitPath)
            transform(Matrix().apply { scale(x = size.width, y = size.height) })
        }
    val measure = PathMeasure()
    measure.setPath(scaled, false)
    val segment = Path()
    measure.getSegment(0f, measure.length * progress, segment, startWithMoveTo = true)
    drawPath(path = segment, color = color, style = Stroke(width = DrawStrokeWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** A checkmark path trimmed in by [animationSpec]: `tweens.pathDraw`'s demo (MOT-05's check). */
@Composable
internal fun PathDrawRow(
    label: String,
    animationSpec: FiniteAnimationSpec<Float>,
    modifier: Modifier = Modifier,
) {
    val target = rememberAutoPlayState()
    val progress by animateFloatAsState(targetValue = if (target.value) 1f else 0f, animationSpec = animationSpec)
    val color = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier.clickable { target.value = !target.value },
        verticalArrangement = Arrangement.spacedBy(RepzSpacing.xxs),
    ) {
        CatalogSectionLabel(label)
        Canvas(modifier = Modifier.size(DrawCanvasSize)) { drawPathProgress(CheckPathUnit, progress, color) }
    }
}

/** A zig-zag "chart" line trimmed in by [animationSpec]: `tweens.chartDraw`'s demo. */
@Composable
internal fun ChartDrawRow(
    label: String,
    animationSpec: FiniteAnimationSpec<Float>,
    modifier: Modifier = Modifier,
) {
    val target = rememberAutoPlayState()
    val progress by animateFloatAsState(targetValue = if (target.value) 1f else 0f, animationSpec = animationSpec)
    val color = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier.clickable { target.value = !target.value },
        verticalArrangement = Arrangement.spacedBy(RepzSpacing.xxs),
    ) {
        CatalogSectionLabel(label)
        Canvas(modifier = Modifier.size(DrawCanvasSize)) { drawPathProgress(ChartPathUnit, progress, color) }
    }
}

/** 8 chips staggering in at [RepzStagger.listPerItem]: the list-stagger demo. */
@Composable
internal fun StaggerChipRow(modifier: Modifier = Modifier) {
    val target = rememberAutoPlayState()

    Column(
        modifier = modifier.clickable { target.value = !target.value },
        verticalArrangement = Arrangement.spacedBy(RepzSpacing.xxs),
    ) {
        CatalogSectionLabel("stagger.listPerItem (${RepzStagger.listPerItem.inWholeMilliseconds} ms/item)")
        Row(horizontalArrangement = Arrangement.spacedBy(RepzSpacing.xs)) {
            repeat(RepzStagger.LIST_MAX_ITEMS) { index ->
                val delayMillis = (RepzStagger.listPerItem * index).inWholeMilliseconds.toInt()
                AnimatedVisibility(
                    visible = target.value,
                    enter = fadeIn(tween(delayMillis = delayMillis)) + scaleIn(tween(delayMillis = delayMillis)),
                    exit = fadeOut(),
                ) {
                    Box(Modifier.size(StaggerChipSize).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape))
                }
            }
        }
    }
}

/** A tile looping [morph] between its start and end shape, driven by [animationSpec]: the
 * motion-morphs page's row. */
@Composable
internal fun MorphTile(
    label: String,
    morph: Morph,
    animationSpec: FiniteAnimationSpec<Float>,
    modifier: Modifier = Modifier,
) {
    val target = rememberAutoPlayState()
    val progress by animateFloatAsState(targetValue = if (target.value) 1f else 0f, animationSpec = animationSpec)

    Column(
        modifier = modifier.clickable { target.value = !target.value },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RepzSpacing.xxs),
    ) {
        Box(
            modifier =
                Modifier
                    .size(CatalogTileSize)
                    .background(MaterialTheme.colorScheme.primaryContainer, RepzMorphShape(morph, progress)),
        )
        CatalogSectionLabel(label)
    }
}
