package com.repz.app.core.designsystem.component

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.repz.app.core.designsystem.generated.resources.Res
import com.repz.app.core.designsystem.generated.resources.designsystem_loading
import com.repz.app.core.designsystem.motion.LocalReducedMotion
import com.repz.app.core.designsystem.motion.RepzMotion
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration
import kotlin.time.TimeSource

// U4: the shimmer band's half-width, an exact visual choice (not a board measurement).
private val ShimmerBandHalfWidth: Dp = 48.dp

// U4: a diagonal `onSurface` band at 8 % alpha, soft at both edges.
private const val SHIMMER_BAND_ALPHA = 0.08f
private const val SHIMMER_BAND_START_STOP = 0f
private const val SHIMMER_BAND_PEAK_STOP = 0.5f
private const val SHIMMER_BAND_END_STOP = 1f

/**
 * T10: how much longer [RepzSkeleton] must still wait before showing, given [elapsed] time since its
 * own composition started — a pure twin of the delay [RepzSkeleton] itself runs, so a test can drive
 * it with a fake [TimeSource] instead of a live Compose frame clock.
 */
internal fun remainingSkeletonDelay(
    elapsed: Duration,
    delay: Duration = RepzMotion.durations.skeletonDelay,
): Duration = (delay - elapsed).coerceAtLeast(Duration.ZERO)

/**
 * The scope [RepzSkeleton] passes to its `content`: every bone marked with [skeletonBone] shares one
 * bone colour and one shimmer transition (U4).
 */
interface RepzSkeletonScope {
    fun Modifier.skeletonBone(shape: Shape): Modifier
}

private class RepzSkeletonScopeImpl(
    private val isShimmering: Boolean,
    private val boneColor: Color,
    private val bandColor: Color,
    private val sweepProgress: State<Float>,
) : RepzSkeletonScope {
    override fun Modifier.skeletonBone(shape: Shape): Modifier {
        val based = clip(shape).background(boneColor)
        if (!isShimmering) return based
        return based.drawWithContent {
            drawContent()
            val bandSpanPx = ShimmerBandHalfWidth.toPx()
            val travelPx = size.width + (2 * bandSpanPx)
            val bandCenterPx = (sweepProgress.value * travelPx) - bandSpanPx
            val brush =
                Brush.linearGradient(
                    SHIMMER_BAND_START_STOP to Color.Transparent,
                    SHIMMER_BAND_PEAK_STOP to bandColor,
                    SHIMMER_BAND_END_STOP to Color.Transparent,
                    start = Offset(bandCenterPx - bandSpanPx, 0f),
                    end = Offset(bandCenterPx + bandSpanPx, size.height),
                )
            drawRect(brush)
        }
    }
}

@Composable
private fun rememberSkeletonSweepProgress(enabled: Boolean): State<Float> {
    if (!enabled) return remember { mutableFloatStateOf(SHIMMER_BAND_START_STOP) }
    val transition = rememberInfiniteTransition(label = "repzSkeletonShimmer")
    return transition.animateFloat(
        initialValue = SHIMMER_BAND_START_STOP,
        targetValue = SHIMMER_BAND_END_STOP,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        durationMillis =
                            RepzMotion.durations.shimmer.inWholeMilliseconds
                                .toInt(),
                        easing = RepzMotion.easing.linear,
                    ),
            ),
        label = "repzSkeletonShimmerProgress",
    )
}

/**
 * A loading placeholder (DESIGN_SYSTEM §9, board 11) for any list or card whose layout is already
 * known: [content] builds that real layout, marking each placeholder with
 * [RepzSkeletonScope.skeletonBone] (`surfaceContainerHighest`, U4) instead of real content. Renders
 * nothing for the first `RepzMotion.durations.skeletonDelay` (150 ms, T10) of its own composition —
 * so a fast load never flashes a skeleton — then shows with no fade. A single shared
 * `RepzMotion.durations.shimmer` (1.2 s linear) diagonal `onSurface` band at 8 % alpha sweeps every
 * bone together; reduced motion drops the band and leaves the bones static. [contentDescription] is
 * the skeleton's one accessibility node; the bones themselves carry none.
 */
@Composable
fun RepzSkeleton(
    modifier: Modifier = Modifier,
    contentDescription: String = stringResource(Res.string.designsystem_loading),
    content: @Composable RepzSkeletonScope.() -> Unit,
) {
    val startMark = remember { TimeSource.Monotonic.markNow() }
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(startMark) {
        delay(remainingSkeletonDelay(startMark.elapsedNow()))
        isVisible = true
    }
    if (!isVisible) return

    RepzSkeletonBody(modifier = modifier, contentDescription = contentDescription, content = content)
}

/**
 * The always-visible bones, factored out of [RepzSkeleton] so a `@Preview` can show them directly
 * instead of waiting through the 150 ms delay (T10).
 */
@Composable
internal fun RepzSkeletonBody(
    modifier: Modifier = Modifier,
    contentDescription: String = stringResource(Res.string.designsystem_loading),
    content: @Composable RepzSkeletonScope.() -> Unit,
) {
    val reducedMotion = LocalReducedMotion.current
    val boneColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val bandColor = MaterialTheme.colorScheme.onSurface.copy(alpha = SHIMMER_BAND_ALPHA)
    val sweepProgress = rememberSkeletonSweepProgress(enabled = !reducedMotion)
    val scope =
        remember(reducedMotion, boneColor, bandColor, sweepProgress) {
            RepzSkeletonScopeImpl(
                isShimmering = !reducedMotion,
                boneColor = boneColor,
                bandColor = bandColor,
                sweepProgress = sweepProgress,
            )
        }

    Box(modifier = modifier.clearAndSetSemantics { this.contentDescription = contentDescription }) {
        scope.content()
    }
}
