package com.repz.app.core.designsystem.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.repz.app.core.designsystem.motion.RepzMotion
import com.repz.app.core.designsystem.motion.RepzSpringTokens
import com.repz.app.core.designsystem.theme.RepzSpacing

/** `motion-springs-2` (MOTION.md §2): the logo springs — 5 rows, fitting one iPhone Duo screen.
 * `tweens.*` and the list stagger moved to `motion-tweens` (they didn't fit alongside these on
 * iPhone Duo; see [MotionTweensCatalogPage]). */
@Composable
internal fun MotionSpringsCatalogPage2() {
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.md)) {
        ReducedMotionHeader()
        SpringDotRow(springLabel("logoBar", RepzSpringTokens.logoBar), RepzMotion.springs.logoBar())
        SpringDotRow(springLabel("logoPlate", RepzSpringTokens.logoPlate), RepzMotion.springs.logoPlate())
        SpringDotRow(springLabel("logoPlatePr", RepzSpringTokens.logoPlatePr), RepzMotion.springs.logoPlatePr())
        SpringDotRow(springLabel("logoImpact", RepzSpringTokens.logoImpact), RepzMotion.springs.logoImpact())
        SpringDotRow(springLabel("logoWordmark", RepzSpringTokens.logoWordmark), RepzMotion.springs.logoWordmark())
    }
}
