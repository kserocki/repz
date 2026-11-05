package com.repz.app.shared.shell.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import com.repz.app.core.designsystem.logo.RepzLaunchLogo
import com.repz.app.core.designsystem.logo.RepzLaunchLogoPlayback
import com.repz.app.core.designsystem.motion.RepzMotion
import com.repz.app.shared.shell.LaunchHandoffPhase
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * The launch hand-off's visuals (ADR-0025 amendment): [BrandBackground] plus [RepzLaunchLogo],
 * starting from the bar-only frame that matches the OS splash. [phase] drives what plays:
 * [LaunchHandoffPhase.HoldingSplashFrame] keeps the static bar; [LaunchHandoffPhase.LoadingTheBar]
 * plays MOT-01 once and calls [onLogoFinish] when it settles (or is skipped); once that happens
 * (or straight away for a signed-in cold start), [LaunchHandoffPhase.FadingOut] fades this whole
 * layer out over `tweens.medium` to reveal the destination underneath — already composed, so the
 * fade is an overlapping crossfade, never a blank frame — and calls [onFadeFinish].
 *
 * Until [LaunchHandoffPhase.FadingOut] starts, this layer is visually opaque (brand background +
 * logo) over whatever the caller has already composed beneath it: it consumes every pointer event
 * across the full window, so nothing underneath is reachable by touch, and a plain tap anywhere on
 * it — not only over the logo's own bounds — skips MOT-01 straight to its finished frame. The
 * caller is expected to also hide that same content from TalkBack/VoiceOver while this layer is
 * opaque (e.g. `Modifier.semantics { hideFromAccessibility() }`), since semantics visibility isn't
 * something a sibling composable can do on another sibling's behalf.
 *
 * [LaunchHandoffPhase.Done] is never passed in: the caller stops composing this layer once reached.
 */
@Composable
internal fun LaunchHandoff(
    phase: LaunchHandoffPhase,
    onLogoFinish: () -> Unit,
    onFadeFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Once MOT-01 has started, it keeps playing (or stays on its finished frame) through FadingOut,
    // even though phase itself moves on; a signed-in cold start never sets this, so FadingOut there
    // keeps fading the plain static bar.
    var hasPlayedLoadTheBar by remember { mutableStateOf(false) }
    LaunchedEffect(phase) {
        if (phase == LaunchHandoffPhase.LoadingTheBar) hasPlayedLoadTheBar = true
    }
    val playback = if (hasPlayedLoadTheBar) RepzLaunchLogoPlayback.LoadTheBar else RepzLaunchLogoPlayback.StaticSymbol

    val fadeAlpha = remember { Animatable(1f) }
    val fadeOutSpec = RepzMotion.tweens.medium<Float>()
    val currentOnFadeFinish by rememberUpdatedState(onFadeFinish)
    LaunchedEffect(phase) {
        if (phase == LaunchHandoffPhase.FadingOut) {
            fadeAlpha.animateTo(0f, fadeOutSpec)
            currentOnFadeFinish()
        }
    }

    val skipRequests = remember { MutableSharedFlow<Unit>(extraBufferCapacity = 1) }
    val isBlockingTouches = phase != LaunchHandoffPhase.FadingOut
    val touchBlockingModifier =
        if (isBlockingTouches) {
            Modifier.pointerInput(Unit) { blockTouchesAndDetectTaps(onTap = { skipRequests.tryEmit(Unit) }) }
        } else {
            Modifier
        }

    Box(modifier = modifier.fillMaxSize().then(touchBlockingModifier).graphicsLayer { alpha = fadeAlpha.value }) {
        BrandBackground()
        Box(modifier = Modifier.align(Alignment.Center)) {
            RepzLaunchLogo(playback = playback, onFinish = onLogoFinish, skipRequests = skipRequests)
        }
    }
}

/**
 * Consumes every pointer event at [PointerEventPass.Initial] — before any sibling underneath this
 * layer (hit-tested first, since this layer is drawn on top) ever sees it — so a tap, drag or any
 * other gesture never reaches the content the launch layer visually covers. A gesture that moves
 * less than [PointerInputScope.viewConfiguration]'s touch slop still counts as [onTap]: the skip
 * gesture must work anywhere on this layer, not only over the logo's own bounds.
 */
private suspend fun PointerInputScope.blockTouchesAndDetectTaps(onTap: () -> Unit) {
    awaitEachGesture {
        var totalDrag = 0f
        var event = awaitPointerEvent(PointerEventPass.Initial)
        while (true) {
            event.changes.forEach { change ->
                totalDrag += change.positionChange().getDistance()
                change.consume()
            }
            if (event.changes.none { it.pressed }) break
            event = awaitPointerEvent(PointerEventPass.Initial)
        }
        if (totalDrag < viewConfiguration.touchSlop) onTap()
    }
}
