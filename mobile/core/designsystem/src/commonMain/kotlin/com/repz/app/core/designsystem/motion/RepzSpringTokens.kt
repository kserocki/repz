package com.repz.app.core.designsystem.motion

/**
 * One row of MOTION.md §2's spring table: a `dampingRatio`/`stiffness` pair plus whether it
 * animates shape or position ([RepzSpringKind.Spatial]) or colour/alpha ([RepzSpringKind.Effects]).
 * [RepzSprings] uses [kind] to pick each spring's reduced-motion fallback.
 */
internal class RepzSpringToken(
    val dampingRatio: Float,
    val stiffness: Float,
    val kind: RepzSpringKind,
)

internal enum class RepzSpringKind { Spatial, Effects }

/** The 13 springs from MOTION.md §2, by name. Every token but `effects*` is [RepzSpringKind.Spatial]
 * (including `celebrate`, `pressMorph` and every `logo*`), even `logoWordmark`'s critically-damped
 * 1.0 ratio: the classification is about what the spring animates, not how bouncy it is. */
internal object RepzSpringTokens {
    val spatialFast = RepzSpringToken(dampingRatio = 0.9f, stiffness = 1400f, kind = RepzSpringKind.Spatial)
    val spatialDefault = RepzSpringToken(dampingRatio = 0.9f, stiffness = 700f, kind = RepzSpringKind.Spatial)
    val spatialSlow = RepzSpringToken(dampingRatio = 0.9f, stiffness = 300f, kind = RepzSpringKind.Spatial)
    val effectsFast = RepzSpringToken(dampingRatio = 1.0f, stiffness = 3800f, kind = RepzSpringKind.Effects)
    val effectsDefault = RepzSpringToken(dampingRatio = 1.0f, stiffness = 1600f, kind = RepzSpringKind.Effects)
    val effectsSlow = RepzSpringToken(dampingRatio = 1.0f, stiffness = 800f, kind = RepzSpringKind.Effects)
    val celebrate = RepzSpringToken(dampingRatio = 0.55f, stiffness = 380f, kind = RepzSpringKind.Spatial)
    val pressMorph = RepzSpringToken(dampingRatio = 0.6f, stiffness = 1200f, kind = RepzSpringKind.Spatial)
    val logoBar = RepzSpringToken(dampingRatio = 0.8f, stiffness = 600f, kind = RepzSpringKind.Spatial)
    val logoPlate = RepzSpringToken(dampingRatio = 0.45f, stiffness = 500f, kind = RepzSpringKind.Spatial)
    val logoPlatePr = RepzSpringToken(dampingRatio = 0.4f, stiffness = 500f, kind = RepzSpringKind.Spatial)
    val logoImpact = RepzSpringToken(dampingRatio = 0.5f, stiffness = 800f, kind = RepzSpringKind.Spatial)
    val logoWordmark = RepzSpringToken(dampingRatio = 1.0f, stiffness = 380f, kind = RepzSpringKind.Spatial)
}
