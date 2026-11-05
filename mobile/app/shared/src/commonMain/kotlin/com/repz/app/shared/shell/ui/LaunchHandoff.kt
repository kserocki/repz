package com.repz.app.shared.shell.ui

import androidx.compose.animation.core.Animatable
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
import com.repz.app.core.designsystem.logo.RepzLaunchLogo
import com.repz.app.core.designsystem.logo.RepzLaunchLogoPlayback
import com.repz.app.core.designsystem.motion.RepzMotion
import com.repz.app.shared.shell.LaunchHandoffPhase

/**
 * The launch hand-off's visuals (ADR-0025 amendment): [BrandBackground] plus [RepzLaunchLogo],
 * starting from the bar-only frame that matches the OS splash. [phase] drives what plays:
 * [LaunchHandoffPhase.HoldingSplashFrame] keeps the static bar; [LaunchHandoffPhase.LoadingTheBar]
 * plays MOT-01 once and calls [onLogoFinish] when it settles (or is skipped); once that happens
 * (or straight away for a signed-in cold start), [LaunchHandoffPhase.FadingOut] fades this whole
 * layer out over `tweens.medium` to reveal the destination underneath — already composed, so the
 * fade is an overlapping crossfade, never a blank frame — and calls [onFadeFinish].
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

    Box(modifier = modifier.fillMaxSize().graphicsLayer { alpha = fadeAlpha.value }) {
        BrandBackground()
        Box(modifier = Modifier.align(Alignment.Center)) {
            RepzLaunchLogo(playback = playback, onFinish = onLogoFinish)
        }
    }
}
