package com.repz.app.core.designsystem.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.repz.app.core.designsystem.motion.RepzMotion
import com.repz.app.core.designsystem.motion.RepzSpringTokens
import com.repz.app.core.designsystem.theme.RepzSpacing

/** `motion-springs-2` (MOTION.md §2): the logo springs, `tweens.short`/`medium`, the draw tweens
 * and the list stagger — 10 rows, fitting one iPhone Duo screen. */
@Composable
internal fun MotionSpringsCatalogPage2() {
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.md)) {
        ReducedMotionHeader()
        SpringDotRow(springLabel("logoBar", RepzSpringTokens.logoBar), RepzMotion.springs.logoBar())
        SpringDotRow(springLabel("logoPlate", RepzSpringTokens.logoPlate), RepzMotion.springs.logoPlate())
        SpringDotRow(springLabel("logoPlatePr", RepzSpringTokens.logoPlatePr), RepzMotion.springs.logoPlatePr())
        SpringDotRow(springLabel("logoImpact", RepzSpringTokens.logoImpact), RepzMotion.springs.logoImpact())
        SpringDotRow(springLabel("logoWordmark", RepzSpringTokens.logoWordmark), RepzMotion.springs.logoWordmark())
        FadeRow("tweens.short", RepzMotion.tweens.short())
        FadeRow("tweens.medium", RepzMotion.tweens.medium())
        PathDrawRow("tweens.pathDraw", RepzMotion.tweens.pathDraw())
        ChartDrawRow("tweens.chartDraw", RepzMotion.tweens.chartDraw())
        StaggerChipRow()
    }
}
