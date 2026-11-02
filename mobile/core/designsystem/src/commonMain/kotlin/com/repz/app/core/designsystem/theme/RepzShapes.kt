package com.repz.app.core.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon

/** The M3 corner-size scale (DESIGN_SYSTEM §5), passed to `RepzTheme`'s `MaterialExpressiveTheme`
 * and read back through `MaterialTheme.shapes`. */
internal val RepzMaterialShapes: Shapes =
    Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(28.dp),
        largeIncreased = RoundedCornerShape(20.dp),
        extraLargeIncreased = RoundedCornerShape(32.dp),
        extraExtraLarge = RoundedCornerShape(48.dp),
    )

/**
 * `none`/`full` plus the 9 accent [RoundedPolygon]s DESIGN_SYSTEM §5 names (aliases of
 * [MaterialShapes]). Callers turn a polygon into a [Shape] with
 * `androidx.compose.material3.toShape()`. Morph pairs (§5) belong to the motion work.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
object RepzShapes {
    val none: Shape = RectangleShape
    val full: CornerBasedShape = CircleShape

    val pill: RoundedPolygon = MaterialShapes.Pill
    val cookie4Sided: RoundedPolygon = MaterialShapes.Cookie4Sided
    val cookie9Sided: RoundedPolygon = MaterialShapes.Cookie9Sided
    val sunny: RoundedPolygon = MaterialShapes.Sunny
    val softBurst: RoundedPolygon = MaterialShapes.SoftBurst
    val burst: RoundedPolygon = MaterialShapes.Burst
    val gem: RoundedPolygon = MaterialShapes.Gem
    val clover4Leaf: RoundedPolygon = MaterialShapes.Clover4Leaf
    val flower: RoundedPolygon = MaterialShapes.Flower
}
