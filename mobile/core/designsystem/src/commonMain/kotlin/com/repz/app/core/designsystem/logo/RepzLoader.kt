package com.repz.app.core.designsystem.logo

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.progressSemantics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.repz.app.core.designsystem.motion.LocalReducedMotion
import com.repz.app.core.designsystem.motion.RepzMotion
import com.repz.app.core.designsystem.motion.RepzStagger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

private val LoopHold = 300.milliseconds
private val SmallOutlineStrokeWidth = 1.dp
private val DefaultLoaderOutlineStrokeWidth = 1.5.dp

/**
 * The branded loader (MOT-25, brand/LOGO.md §5 "Reuse inside the app"): the bar is always drawn,
 * the 3 plate pairs load pair by pair. [progress] `null` loops indeterminately (every pair in turn,
 * [RepzStagger.logoPlatesPerPair] apart); otherwise `0f..1f` locks one pair per third (MOT-18):
 * locked pairs are solid and still, the next pair keeps looping, pairs still to come are drawn as a
 * dashed outline. [contentDescription] is required (unlike [RepzLogoSymbol]'s, which may be purely
 * decorative): a loader is never only decorative.
 *
 * Reduced motion: indeterminate shows the static, fully assembled symbol; determinate snaps locked
 * pairs in solid and leaves the rest as dashed outlines, with no loop and no sliding (MOT-25).
 */
@Composable
fun RepzLoader(
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: RepzLoaderSize = RepzLoaderSize.Medium,
    progress: Float? = null,
    color: Color = RepzLogoDefaults.loaderColor(),
) {
    val reducedMotion = LocalReducedMotion.current
    val state = rememberRepzLogoSymbolState(initialPose = RepzLogoPose.BarOnly)
    val slideSpec = RepzMotion.springs.logoPlate<Float>()
    val fadeInSpec = RepzMotion.effectsSprings.effectsFast<Float>()
    val fadeOutSpec = RepzMotion.effectsSprings.effectsDefault<Float>()
    val lockSpec = RepzMotion.effectsSprings.effectsFast<Float>()

    if (reducedMotion) {
        LaunchedEffect(progress) {
            if (progress == null) {
                state.snapTo(RepzLogoPose.Assembled)
                RepzPlatePair.entries.forEach { state.isOutlined.getValue(it).snapTo(0f) }
            } else {
                val states = pairStates(progress)
                for (pair in RepzPlatePair.entries) {
                    if (states.getValue(pair) == PairState.Locked) state.snapPairLocked(pair) else state.snapPairDashed(pair)
                }
            }
        }
    } else if (progress == null) {
        LaunchedEffect(Unit) { loopIndeterminate(state, slideSpec, fadeInSpec, fadeOutSpec) }
    } else {
        val states = pairStates(progress)
        for (pair in RepzPlatePair.entries) {
            key(pair) {
                val pairState = states.getValue(pair)
                LaunchedEffect(pairState) {
                    when (pairState) {
                        PairState.Locked -> lockPairIn(state, pair, slideSpec, lockSpec)
                        PairState.Loading -> loopSinglePair(state, pair, slideSpec, fadeInSpec, fadeOutSpec)
                        PairState.Pending -> state.snapPairDashed(pair)
                    }
                }
            }
        }
    }

    val progressModifier =
        if (progress != null) Modifier.progressSemantics(progress.coerceIn(0f, 1f)) else Modifier.progressSemantics()

    RepzLogoSymbolCanvas(
        state = state,
        color = color,
        modifier =
            modifier
                .width(size.width)
                .then(progressModifier)
                .semantics { this.contentDescription = contentDescription },
        plateDistancePx = ::oneLocalPlateWidthDistancePx,
        outlineStrokeWidth = if (size == RepzLoaderSize.Small) SmallOutlineStrokeWidth else DefaultLoaderOutlineStrokeWidth,
    )
}

/** MOT-25/Q4: the loader slides a pair in from its own width away, not the real screen edge
 * ([RepzLogoSymbol]'s window-relative [plateEntryDistancePx]). */
private fun oneLocalPlateWidthDistancePx(
    part: RepzLogoPart,
    artworkWidthPx: Float,
): Float {
    val width = partRectPx(part, artworkWidthPx).width
    return if (part.isLeftSide) -width else width
}

private suspend fun RepzLogoSymbolState.snapPairLocked(pair: RepzPlatePair) {
    plateEntry.getValue(pair).snapTo(0f)
    plateAlpha.getValue(pair).snapTo(1f)
    isOutlined.getValue(pair).snapTo(0f)
}

private suspend fun RepzLogoSymbolState.snapPairDashed(pair: RepzPlatePair) {
    plateEntry.getValue(pair).snapTo(1f)
    plateAlpha.getValue(pair).snapTo(1f)
    isOutlined.getValue(pair).snapTo(1f)
}

/** The determinate loop's one-shot "a pair just locked" transition: slides to rest, settles opaque
 * and solid (brand/LOGO.md §5, MOT-18). */
private suspend fun lockPairIn(
    state: RepzLogoSymbolState,
    pair: RepzPlatePair,
    slideSpec: FiniteAnimationSpec<Float>,
    outlineSpec: FiniteAnimationSpec<Float>,
) {
    coroutineScope {
        launch { state.plateEntry.getValue(pair).animateTo(0f, slideSpec) }
        launch { state.plateAlpha.getValue(pair).animateTo(1f, outlineSpec) }
        launch { state.isOutlined.getValue(pair).animateTo(0f, outlineSpec) }
    }
}

/** All 3 pairs loop in turn, [RepzStagger.logoPlatesPerPair] apart (Q4): each pair's own cycle
 * takes the same time (the same specs, the same [LoopHold]), so the stagger never drifts. */
private suspend fun loopIndeterminate(
    state: RepzLogoSymbolState,
    slideSpec: FiniteAnimationSpec<Float>,
    fadeInSpec: FiniteAnimationSpec<Float>,
    fadeOutSpec: FiniteAnimationSpec<Float>,
) {
    RepzPlatePair.entries.forEach { state.isOutlined.getValue(it).snapTo(0f) }
    coroutineScope {
        RepzPlatePair.entries.forEachIndexed { index, pair ->
            launch {
                delay(RepzStagger.logoPlatesPerPair * index)
                loopSinglePair(state, pair, slideSpec, fadeInSpec, fadeOutSpec)
            }
        }
    }
}

/** One pair's indeterminate loop, and the determinate loader's single "next" ([PairState.Loading])
 * pair: slide in + fade in, hold, fade out together, repeat (Q4). A [CoroutineScope] extension only
 * so [isActive] resolves; every call site is already inside one ([LaunchedEffect] or [launch]). */
private suspend fun CoroutineScope.loopSinglePair(
    state: RepzLogoSymbolState,
    pair: RepzPlatePair,
    slideSpec: FiniteAnimationSpec<Float>,
    fadeInSpec: FiniteAnimationSpec<Float>,
    fadeOutSpec: FiniteAnimationSpec<Float>,
) {
    val entry = state.plateEntry.getValue(pair)
    val alpha = state.plateAlpha.getValue(pair)
    state.isOutlined.getValue(pair).snapTo(0f)
    while (isActive) {
        entry.snapTo(1f)
        alpha.snapTo(0f)
        coroutineScope {
            launch { entry.animateTo(0f, slideSpec) }
            launch { alpha.animateTo(1f, fadeInSpec) }
        }
        delay(LoopHold)
        alpha.animateTo(0f, fadeOutSpec)
    }
}
