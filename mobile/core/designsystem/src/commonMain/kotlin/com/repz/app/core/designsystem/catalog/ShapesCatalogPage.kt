package com.repz.app.core.designsystem.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import com.repz.app.core.designsystem.theme.RepzShapes
import com.repz.app.core.designsystem.theme.RepzSpacing

/** The 10 shape-scale steps (DESIGN_SYSTEM §5) as `surfaceContainerHighest` tiles, plus the 9
 * accent polygons as `primaryContainer` tiles. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ShapesCatalogPage() {
    val scheme = MaterialTheme.colorScheme
    val shapes = MaterialTheme.shapes

    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.lg)) {
        CatalogSectionLabel("scale (10)")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
            ShapeTile("none", RepzShapes.none, scheme.surfaceContainerHighest)
            ShapeTile("extraSmall", shapes.extraSmall, scheme.surfaceContainerHighest)
            ShapeTile("small", shapes.small, scheme.surfaceContainerHighest)
            ShapeTile("medium", shapes.medium, scheme.surfaceContainerHighest)
            ShapeTile("large", shapes.large, scheme.surfaceContainerHighest)
            ShapeTile("largeIncreased", shapes.largeIncreased, scheme.surfaceContainerHighest)
            ShapeTile("extraLarge", shapes.extraLarge, scheme.surfaceContainerHighest)
            ShapeTile("extraLargeIncreased", shapes.extraLargeIncreased, scheme.surfaceContainerHighest)
            ShapeTile("extraExtraLarge", shapes.extraExtraLarge, scheme.surfaceContainerHighest)
            ShapeTile("full", RepzShapes.full, scheme.surfaceContainerHighest)
        }

        CatalogSectionLabel("accents (9)")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
            ShapeTile("pill", RepzShapes.pill.toShape(), scheme.primaryContainer)
            ShapeTile("cookie4Sided", RepzShapes.cookie4Sided.toShape(), scheme.primaryContainer)
            ShapeTile("cookie9Sided", RepzShapes.cookie9Sided.toShape(), scheme.primaryContainer)
            ShapeTile("sunny", RepzShapes.sunny.toShape(), scheme.primaryContainer)
            ShapeTile("softBurst", RepzShapes.softBurst.toShape(), scheme.primaryContainer)
            ShapeTile("burst", RepzShapes.burst.toShape(), scheme.primaryContainer)
            ShapeTile("gem", RepzShapes.gem.toShape(), scheme.primaryContainer)
            ShapeTile("clover4Leaf", RepzShapes.clover4Leaf.toShape(), scheme.primaryContainer)
            ShapeTile("flower", RepzShapes.flower.toShape(), scheme.primaryContainer)
        }
    }
}
