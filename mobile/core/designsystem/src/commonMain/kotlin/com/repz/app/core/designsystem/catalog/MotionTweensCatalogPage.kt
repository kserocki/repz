package com.repz.app.core.designsystem.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.repz.app.core.designsystem.motion.RepzMotion
import com.repz.app.core.designsystem.theme.RepzSpacing

/** `motion-tweens` (MOTION.md §2): `tweens.short`/`medium`, the draw tweens and the list stagger —
 * 5 rows, fitting one iPhone Duo screen. Split out of `motion-springs-2`
 * ([MotionSpringsCatalogPage2]), which overflowed the iPhone Duo screen with all 10 rows together
 * (Spotter, round 2). */
@Composable
internal fun MotionTweensCatalogPage() {
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.md)) {
        ReducedMotionHeader()
        FadeRow("tweens.short", RepzMotion.tweens.short())
        FadeRow("tweens.medium", RepzMotion.tweens.medium())
        PathDrawRow("tweens.pathDraw", RepzMotion.tweens.pathDraw())
        ChartDrawRow("tweens.chartDraw", RepzMotion.tweens.chartDraw())
        StaggerChipRow()
    }
}
