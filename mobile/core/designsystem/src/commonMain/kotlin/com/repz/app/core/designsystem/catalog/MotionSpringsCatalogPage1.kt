package com.repz.app.core.designsystem.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.repz.app.core.designsystem.motion.RepzMotion
import com.repz.app.core.designsystem.motion.RepzSpringToken
import com.repz.app.core.designsystem.motion.RepzSpringTokens
import com.repz.app.core.designsystem.theme.RepzSpacing

/** `name ζ<dampingRatio>/k<stiffness>`, MOTION.md §2's own notation for a spring row's label. */
internal fun springLabel(
    name: String,
    token: RepzSpringToken,
): String = "$name ζ${token.dampingRatio}/k${token.stiffness.toInt()}"

/** `motion-springs-1` (DESIGN_SYSTEM/MOTION.md §2): the spatial springs, `celebrate`/`pressMorph`
 * and the effects springs — 8 rows, each fitting one iPhone Duo screen. */
@Composable
internal fun MotionSpringsCatalogPage1() {
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.md)) {
        ReducedMotionHeader()
        SpringDotRow(springLabel("spatialFast", RepzSpringTokens.spatialFast), RepzMotion.springs.spatialFast())
        SpringDotRow(springLabel("spatialDefault", RepzSpringTokens.spatialDefault), RepzMotion.springs.spatialDefault())
        SpringDotRow(springLabel("spatialSlow", RepzSpringTokens.spatialSlow), RepzMotion.springs.spatialSlow())
        SpringDotRow(springLabel("celebrate", RepzSpringTokens.celebrate), RepzMotion.springs.celebrate())
        SpringDotRow(springLabel("pressMorph", RepzSpringTokens.pressMorph), RepzMotion.springs.pressMorph())
        SwatchFadeRow(springLabel("effectsFast", RepzSpringTokens.effectsFast), RepzMotion.springs.effectsFast())
        SwatchFadeRow(springLabel("effectsDefault", RepzSpringTokens.effectsDefault), RepzMotion.springs.effectsDefault())
        SwatchFadeRow(springLabel("effectsSlow", RepzSpringTokens.effectsSlow), RepzMotion.springs.effectsSlow())
    }
}
