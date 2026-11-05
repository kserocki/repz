package com.repz.app.core.designsystem.logo

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember

/**
 * [RepzLogoSymbol]'s rest poses. The OS splash shows only the bar (never the full symbol), so the
 * bar itself has no pose of its own and is always drawn at full scale; a pose only says where the
 * 3 plate pairs sit.
 */
enum class RepzLogoPose {
    /** The bar alone, every plate pair fully retracted past its own screen edge: the splash hand-off
     * frame and [RepzLaunchLogoPlayback.StaticSymbol]'s rest state. */
    BarOnly,

    /** Every part at its rest position: the finished MOT-01 frame and the catalog's static symbol. */
    Assembled,
}

private val RepzLogoPose.plateEntry: Float
    get() =
        when (this) {
            RepzLogoPose.BarOnly -> 1f
            RepzLogoPose.Assembled -> 0f
        }

/**
 * [RepzLogoSymbol]'s animatable state: one [symbolScaleY] shared by the whole symbol (the landing
 * impact squash), and 3 per-[RepzPlatePair] animatables each for [plateEntry] (0 = rest, 1 = at
 * entry start; the spring's overshoot can go below 0), [plateAlpha] (the loader's fade) and
 * [isOutlined] (the loader's dashed-pending look). Driven by [RepzLaunchLogo] (MOT-01) or
 * [RepzLoader] (MOT-25); a caller displaying a plain static symbol never touches it beyond
 * [rememberRepzLogoSymbolState]'s initial pose.
 */
@Stable
class RepzLogoSymbolState internal constructor(
    initialPose: RepzLogoPose,
) {
    internal val symbolScaleY = Animatable(1f)

    internal val plateEntry: Map<RepzPlatePair, Animatable<Float, AnimationVector1D>> =
        RepzPlatePair.entries.associateWith { Animatable(initialPose.plateEntry) }

    internal val plateAlpha: Map<RepzPlatePair, Animatable<Float, AnimationVector1D>> =
        RepzPlatePair.entries.associateWith { Animatable(1f) }

    internal val isOutlined: Map<RepzPlatePair, Animatable<Float, AnimationVector1D>> =
        RepzPlatePair.entries.associateWith { Animatable(0f) }

    /** Jumps every animatable straight to [pose], with no spring: a tap-skip or the reduced-motion
     * rest state. */
    suspend fun snapTo(pose: RepzLogoPose) {
        symbolScaleY.snapTo(1f)
        plateEntry.values.forEach { it.snapTo(pose.plateEntry) }
        plateAlpha.values.forEach { it.snapTo(1f) }
        isOutlined.values.forEach { it.snapTo(0f) }
    }
}

@Composable
fun rememberRepzLogoSymbolState(initialPose: RepzLogoPose = RepzLogoPose.Assembled): RepzLogoSymbolState =
    remember(initialPose) { RepzLogoSymbolState(initialPose) }
