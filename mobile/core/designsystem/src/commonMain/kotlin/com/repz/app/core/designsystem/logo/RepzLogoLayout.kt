package com.repz.app.core.designsystem.logo

import androidx.compose.ui.geometry.Rect
import com.repz.app.core.designsystem.motion.RepzStagger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** [RepzLogoGeometry.artworkBounds]'s width-to-height ratio (brand/LOGO.md §2: 296 : 128, i.e.
 * 2.3125 : 1), the aspect [RepzLogoSymbol] sizes itself by. */
internal val RepzLogoGeometry.artworkAspectRatio: Float
    get() = artworkBounds.width / artworkBounds.height

/**
 * [part]'s rect in pixels, scaled from the 320x160 grid so that [RepzLogoGeometry.artworkBounds]
 * is [artworkWidthPx] wide, with the origin at the artwork's own top-left (not the grid's): a
 * caller sizing its canvas to the artwork's width can draw this rect directly.
 */
internal fun partRectPx(
    part: RepzLogoPart,
    artworkWidthPx: Float,
): Rect {
    val bounds = RepzLogoGeometry.artworkBounds
    val scale = artworkWidthPx / bounds.width
    val rect = RepzLogoGeometry.parts.getValue(part)
    return Rect(
        left = (rect.x - bounds.x) * scale,
        top = (rect.y - bounds.y) * scale,
        right = (rect.x + rect.width - bounds.x) * scale,
        bottom = (rect.y + rect.height - bounds.y) * scale,
    )
}

/**
 * The pixel distance that, added to [part]'s rest position, puts it fully past its own screen
 * edge: negative (leftward) for a left-side part, positive (rightward) for a right-side part
 * (brand/LOGO.md §5). [symbolLeftInWindowPx] is where the artwork's left edge sits in the window
 * (e.g. from `onGloballyPositioned`/`onPlaced`), [windowWidthPx] the window's own width (e.g.
 * `LocalWindowInfo.current.containerSize.width`); both in the same pixel units as [artworkWidthPx].
 * Meaningless for [RepzLogoPart.Bar] (returns `0f`): the bar never translates.
 */
internal fun plateEntryDistancePx(
    part: RepzLogoPart,
    symbolLeftInWindowPx: Float,
    artworkWidthPx: Float,
    windowWidthPx: Float,
): Float {
    if (part == RepzLogoPart.Bar) return 0f
    val rect = partRectPx(part, artworkWidthPx)
    return if (part.isLeftSide) {
        -(symbolLeftInWindowPx + rect.right)
    } else {
        windowWidthPx - (symbolLeftInWindowPx + rect.left)
    }
}

/** Whether a plate just made first contact at its rest position: [previousEntry] was still short
 * of it (> 0) and [currentEntry] has reached or passed it (<= 0). Used once per landing to trigger
 * the impact squash and the haptic tick (brand/LOGO.md §5); a caller that keeps animating past 0
 * (the spring's overshoot) tracks for itself that it already fired once. */
internal fun hasLanded(
    previousEntry: Float,
    currentEntry: Float,
): Boolean = previousEntry > 0f && currentEntry <= 0f

/** One stage of [launchTimeline]. [RepzLogoPart.Bar] is kept only as the t = 0 anchor: the OS
 * splash already shows the bar, so nothing animates for that stage. */
internal enum class LaunchStage { Bar, InnerPlates, MiddlePlates, OuterPlates, Wordmark }

internal data class TimelineStep(
    val stage: LaunchStage,
    val startAt: Duration,
)

// brand/LOGO.md §5: inner plates start 120 ms after t = 0.
private val BAR_LEAD = 120.milliseconds

// brand/LOGO.md §5: the wordmark rises at 600 ms, not derived from the plate stagger.
private val WORDMARK_START = 600.milliseconds

/** MOT-01's start offsets (brand/LOGO.md §5): bar at 0 (an anchor only), inner plates at 120 ms,
 * then each further pair [RepzStagger.logoPlatesPerPair] later (230, 340 ms), the wordmark at a
 * fixed 600 ms. */
internal fun launchTimeline(): List<TimelineStep> {
    val barStart = Duration.ZERO
    val innerStart = barStart + BAR_LEAD
    val middleStart = innerStart + RepzStagger.logoPlatesPerPair
    val outerStart = middleStart + RepzStagger.logoPlatesPerPair
    return listOf(
        TimelineStep(LaunchStage.Bar, barStart),
        TimelineStep(LaunchStage.InnerPlates, innerStart),
        TimelineStep(LaunchStage.MiddlePlates, middleStart),
        TimelineStep(LaunchStage.OuterPlates, outerStart),
        TimelineStep(LaunchStage.Wordmark, WORDMARK_START),
    )
}

internal val LaunchStage.pair: RepzPlatePair?
    get() =
        when (this) {
            LaunchStage.Bar, LaunchStage.Wordmark -> null
            LaunchStage.InnerPlates -> RepzPlatePair.Inner
            LaunchStage.MiddlePlates -> RepzPlatePair.Middle
            LaunchStage.OuterPlates -> RepzPlatePair.Outer
        }
