package com.repz.app.core.designsystem.motion

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toPath
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.rectangle
import com.repz.app.core.designsystem.theme.RepzShapes

/**
 * The DESIGN_SYSTEM §5 morph pairs, built once from [MaterialShapes.Circle] and the [RepzShapes]
 * accents (both already normalized to the unit square, DESIGN_SYSTEM §2). Callers drive `progress`
 * with an [androidx.compose.animation.core.Animatable] and one of `RepzMotion.springs`, then render
 * the result with [RepzMorphShape].
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
object RepzMorphs {
    /** Rest timer hits 0 (MOT-06). */
    val circleToBurst: Morph by lazy { Morph(MaterialShapes.Circle, RepzShapes.burst) }

    /** Rest breathing, driven both ways by progress (MOT-06). */
    val sunnyToCircle: Morph by lazy { Morph(RepzShapes.sunny, MaterialShapes.Circle) }

    /** PR badge pop-in (MOT-07/08). */
    val circleToGem: Morph by lazy { Morph(MaterialShapes.Circle, RepzShapes.gem) }

    /** Restore progress 0 → 50 % (MOT-18). */
    val circleToFlower: Morph by lazy { Morph(MaterialShapes.Circle, RepzShapes.flower) }

    /** Restore progress 50 → 100 % (MOT-18). */
    val flowerToCookie9Sided: Morph by lazy { Morph(RepzShapes.flower, RepzShapes.cookie9Sided) }

    /**
     * Set checkbox on complete (MOT-05): [cornerRadius] is the end shape's corner radius at
     * [size], e.g. `circleToRoundedSquare(10.dp, 28.dp)` for a 28 dp checkbox.
     */
    fun circleToRoundedSquare(
        cornerRadius: Dp,
        size: Dp,
    ): Morph = Morph(MaterialShapes.Circle, roundedSquare(cornerRadius, size))

    /** FAB press-and-hold (DESIGN_SYSTEM §5): [cornerRadius] is the start shape's corner radius at
     * [size], the FAB's own rest radius. */
    fun roundedSquareToCookie4Sided(
        cornerRadius: Dp,
        size: Dp,
    ): Morph = Morph(roundedSquare(cornerRadius, size), RepzShapes.cookie4Sided)

    /** A unit square, rounded by [cornerRadius] as a fraction of [size], normalized into the same
     * (0,0)-(1,1) coordinate space as [MaterialShapes.Circle] and the [RepzShapes] accents. */
    private fun roundedSquare(
        cornerRadius: Dp,
        size: Dp,
    ): RoundedPolygon =
        RoundedPolygon
            .rectangle(width = 1f, height = 1f, rounding = CornerRounding(radius = cornerRadius / size))
            .normalized()
}

/**
 * Renders a [Morph] at [progress] (`0f` = the start shape, `1f` = the end shape) as a [Shape], the
 * same way `RoundedPolygon.toShape()` renders a single shape: the morph's unit-square path is
 * scaled and centred to the outline size.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Immutable
class RepzMorphShape(
    private val morph: Morph,
    private val progress: Float,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = morph.toPath(progress = progress)
        val scaleMatrix = Matrix().apply { scale(x = size.width, y = size.height) }
        path.transform(scaleMatrix)
        path.translate(size.center - path.getBounds().center)
        return Outline.Generic(path)
    }
}
