package com.repz.app.core.designsystem.logo

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.repz.app.core.designsystem.generated.resources.Res
import com.repz.app.core.designsystem.generated.resources.designsystem_logo_description
import com.repz.app.core.designsystem.generated.resources.logo_wordmark
import com.repz.app.core.designsystem.haptics.RepzHaptics
import com.repz.app.core.designsystem.motion.LocalReducedMotion
import com.repz.app.core.designsystem.motion.RepzDurations
import com.repz.app.core.designsystem.motion.RepzMotion
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.milliseconds

/** [RepzLaunchLogo]'s two playbacks. The OS splash already shows the bar, so [StaticSymbol] is
 * that same static frame (no plates, no wordmark) — the signed-in cold start's
 * hold before it crossfades straight into the app, and the signed-out hold while the session
 * restores. [LoadTheBar] plays MOT-01 (brand/LOGO.md §5) from that same bar: the 3 plate pairs
 * slide in and land in order, then the wordmark rises. */
enum class RepzLaunchLogoPlayback { StaticSymbol, LoadTheBar }

private val WordmarkRestTranslateYDp = 12.dp
private const val IMPACT_SQUASH_SCALE = 0.97f
private val PostWordmarkHold = 300.milliseconds

/**
 * MOT-01 "Load the bar" (brand/LOGO.md §5): [RepzLogoSymbol] at [symbolWidth], the wordmark below
 * it in stacked-lockup proportions. [onFinish] fires once, after the wordmark has settled and
 * [PostWordmarkHold] has passed, or immediately on a tap (which jumps straight to the finished
 * frame — [playback] itself never changes, so a second recomposition does not replay it: the caller
 * owns the phase).
 *
 * Reduced motion ([LocalReducedMotion]): the symbol stays assembled from the first frame, only the
 * wordmark fades in, over [RepzDurations.reducedMotionFade] — brand/LOGO.md §5's "Reduced motion:
 * static logo, 150 ms fade".
 */
@Composable
fun RepzLaunchLogo(
    playback: RepzLaunchLogoPlayback,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = RepzLogoDefaults.color(),
    symbolWidth: Dp = RepzLogoDefaults.SplashSymbolWidth,
) {
    val reducedMotion = LocalReducedMotion.current
    val haptics = RepzHaptics.current
    val state = rememberRepzLogoSymbolState(initialPose = RepzLogoPose.BarOnly)
    val wordmarkAlpha = remember { Animatable(0f) }
    val wordmarkTranslateYDp = remember { Animatable(WordmarkRestTranslateYDp.value) }
    val tapRequests = remember { MutableSharedFlow<Unit>(extraBufferCapacity = 1) }
    val currentOnFinish by rememberUpdatedState(onFinish)

    val logoPlateSpec = RepzMotion.springs.logoPlate<Float>()
    val logoImpactSpec = RepzMotion.springs.logoImpact<Float>()
    val logoWordmarkSpec = RepzMotion.springs.logoWordmark<Float>()
    val reducedWordmarkAlphaSpec = tween<Float>(RepzDurations.reducedMotionFade.inWholeMilliseconds.toInt())

    val runtime =
        remember(state, haptics, wordmarkAlpha, wordmarkTranslateYDp, tapRequests) {
            LaunchLogoRuntime(state, haptics, wordmarkAlpha, wordmarkTranslateYDp, tapRequests)
        }
    val specs = LaunchLogoSpecs(logoPlateSpec, logoImpactSpec, logoWordmarkSpec, reducedWordmarkAlphaSpec)

    LaunchedEffect(playback) {
        playLaunchLogo(playback, reducedMotion, runtime, specs, onFinish = { currentOnFinish() })
    }

    val tapModifier =
        if (playback == RepzLaunchLogoPlayback.LoadTheBar) {
            Modifier.pointerInput(Unit) { detectTapGestures(onTap = { tapRequests.tryEmit(Unit) }) }
        } else {
            Modifier
        }

    val description = stringResource(Res.string.designsystem_logo_description)
    val layout = launchLogoLayout(symbolWidth)

    Column(
        modifier = modifier.then(tapModifier).semantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RepzLogoSymbol(modifier = Modifier.width(symbolWidth), color = color, state = state, contentDescription = null)
        Spacer(modifier = Modifier.height(layout.gapBelowPlates))
        Image(
            painter = painterResource(Res.drawable.logo_wordmark),
            contentDescription = null,
            modifier =
                Modifier
                    .width(layout.wordmarkWidth)
                    .aspectRatio(layout.wordmarkAspectRatio)
                    .graphicsLayer {
                        alpha = wordmarkAlpha.value
                        translationY = wordmarkTranslateYDp.value.dp.toPx()
                    },
            contentScale = ContentScale.FillBounds,
            colorFilter = ColorFilter.tint(color),
        )
    }
}

/** [RepzLaunchLogo]'s stacked-lockup layout (Q8: wordmark at 0.7 scale, centred, 24 grid units
 * under the plates), derived from [symbolWidth] so the symbol and wordmark scale together. Exposed
 * so a preview can render the finished frame without replaying the choreography. */
internal data class LaunchLogoLayout(
    val wordmarkWidth: Dp,
    val wordmarkAspectRatio: Float,
    val gapBelowPlates: Dp,
)

internal fun launchLogoLayout(symbolWidth: Dp): LaunchLogoLayout {
    val unitToDp = symbolWidth / RepzLogoGeometry.artworkBounds.width
    return LaunchLogoLayout(
        wordmarkWidth = unitToDp * (RepzLogoGeometry.WORDMARK_WIDTH * RepzLogoGeometry.STACKED_WORDMARK_SCALE),
        wordmarkAspectRatio = RepzLogoGeometry.WORDMARK_WIDTH / RepzLogoGeometry.WORDMARK_HEIGHT,
        gapBelowPlates =
            unitToDp * (RepzLogoGeometry.STACKED_WORDMARK_TOP - (RepzLogoGeometry.artworkBounds.y + RepzLogoGeometry.artworkBounds.height)),
    )
}

/** The mutable pieces [playLaunchLogo] drives: the shared 7-part [state], the wordmark's own two
 * animatables, [haptics] for the landing tick and [tapRequests] for the skip gesture. */
private class LaunchLogoRuntime(
    val state: RepzLogoSymbolState,
    val haptics: RepzHaptics,
    val wordmarkAlpha: Animatable<Float, AnimationVector1D>,
    val wordmarkTranslateYDp: Animatable<Float, AnimationVector1D>,
    val tapRequests: MutableSharedFlow<Unit>,
)

/** The 4 animation specs [playLaunchLogo] needs, read once in composition (`RepzMotion.*` are
 * `@Composable`) and handed down to its plain suspend helpers. */
private class LaunchLogoSpecs(
    val plate: FiniteAnimationSpec<Float>,
    val impact: FiniteAnimationSpec<Float>,
    val wordmark: FiniteAnimationSpec<Float>,
    val reducedWordmarkAlpha: FiniteAnimationSpec<Float>,
)

/**
 * The whole [RepzLaunchLogo] lifecycle for one [playback]: the static hold, or the choreography
 * (reduced or full) followed by [PostWordmarkHold] and [onFinish] — or a tap cancelling the
 * choreography early and skipping straight to [onFinish], per [RepzLaunchLogo]'s own doc.
 */
private suspend fun playLaunchLogo(
    playback: RepzLaunchLogoPlayback,
    reducedMotion: Boolean,
    runtime: LaunchLogoRuntime,
    specs: LaunchLogoSpecs,
    onFinish: () -> Unit,
) {
    if (playback == RepzLaunchLogoPlayback.StaticSymbol) {
        runtime.state.snapTo(RepzLogoPose.BarOnly)
        runtime.wordmarkAlpha.snapTo(0f)
        runtime.wordmarkTranslateYDp.snapTo(WordmarkRestTranslateYDp.value)
        return
    }

    coroutineScope {
        val choreography =
            launch {
                if (reducedMotion) {
                    runtime.state.snapTo(RepzLogoPose.Assembled)
                    runtime.wordmarkTranslateYDp.snapTo(0f)
                    runtime.wordmarkAlpha.animateTo(1f, specs.reducedWordmarkAlpha)
                } else {
                    playLoadTheBar(runtime, specs)
                }
                delay(PostWordmarkHold)
            }
        val tapListener =
            launch {
                runtime.tapRequests.first()
                choreography.cancel()
            }
        choreography.join()
        tapListener.cancel()
    }

    // Reached either after a natural finish or a tap cancelling the choreography above: land on the
    // finished frame either way, then signal once.
    runtime.state.snapTo(RepzLogoPose.Assembled)
    runtime.wordmarkTranslateYDp.snapTo(0f)
    runtime.wordmarkAlpha.snapTo(1f)
    onFinish()
}

private suspend fun playLoadTheBar(
    runtime: LaunchLogoRuntime,
    specs: LaunchLogoSpecs,
) {
    coroutineScope {
        for (step in launchTimeline()) {
            val pair = step.stage.pair
            when {
                pair != null -> {
                    launch {
                        delay(step.startAt)
                        animatePairIn(runtime, pair, specs)
                    }
                }

                step.stage == LaunchStage.Wordmark -> {
                    launch {
                        delay(step.startAt)
                        launch { runtime.wordmarkTranslateYDp.animateTo(0f, specs.wordmark) }
                        runtime.wordmarkAlpha.animateTo(1f, specs.wordmark)
                    }
                }

                // LaunchStage.Bar: no-op, the bar is already visible from the OS splash.
                else -> {}
            }
        }
    }
}

private suspend fun animatePairIn(
    runtime: LaunchLogoRuntime,
    pair: RepzPlatePair,
    specs: LaunchLogoSpecs,
) {
    val entry = runtime.state.plateEntry.getValue(pair)
    coroutineScope {
        val slideJob = launch { entry.animateTo(0f, specs.plate) }
        awaitLanding(entry)
        runtime.haptics.tick()
        launch {
            runtime.state.symbolScaleY.animateTo(IMPACT_SQUASH_SCALE, specs.impact)
            runtime.state.symbolScaleY.animateTo(1f, specs.impact)
        }
        slideJob.join()
    }
}

/** Suspends until [entry] first crosses its rest position (brand/LOGO.md §5 "each landing"). */
private suspend fun awaitLanding(entry: Animatable<Float, AnimationVector1D>) {
    var previous = entry.value
    snapshotFlow { entry.value }.first { current ->
        val landed = hasLanded(previous, current)
        previous = current
        landed
    }
}
