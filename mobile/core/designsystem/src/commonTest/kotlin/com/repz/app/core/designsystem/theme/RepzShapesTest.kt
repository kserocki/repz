package com.repz.app.core.designsystem.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class RepzShapesTest {
    private val probeSize = Size(100f, 100f)
    private val density = Density(1f)

    @Test
    fun given_repzMaterialShapes_whenResolvingCornerSizes_thenMatchDesignSystemScale() {
        assertEquals(4f, RepzMaterialShapes.extraSmall.topStart.toPx(probeSize, density))
        assertEquals(8f, RepzMaterialShapes.small.topStart.toPx(probeSize, density))
        assertEquals(12f, RepzMaterialShapes.medium.topStart.toPx(probeSize, density))
        assertEquals(16f, RepzMaterialShapes.large.topStart.toPx(probeSize, density))
        assertEquals(20f, RepzMaterialShapes.largeIncreased.topStart.toPx(probeSize, density))
        assertEquals(28f, RepzMaterialShapes.extraLarge.topStart.toPx(probeSize, density))
        assertEquals(32f, RepzMaterialShapes.extraLargeIncreased.topStart.toPx(probeSize, density))
        assertEquals(48f, RepzMaterialShapes.extraExtraLarge.topStart.toPx(probeSize, density))
    }

    @Test
    fun given_repzShapesFull_whenResolvingCornerSize_thenIsFiftyPercent() {
        assertEquals(50f, RepzShapes.full.topStart.toPx(probeSize, density))
    }

    @Test
    fun given_repzShapesAccents_whenReadingPolygons_thenAreTheNineDesignSystemAccents() {
        val accents =
            listOf(
                RepzShapes.pill,
                RepzShapes.cookie4Sided,
                RepzShapes.cookie9Sided,
                RepzShapes.sunny,
                RepzShapes.softBurst,
                RepzShapes.burst,
                RepzShapes.gem,
                RepzShapes.clover4Leaf,
                RepzShapes.flower,
            )

        assertEquals(9, accents.size)
        accents.forEach { polygon -> assertTrue(polygon.cubics.isNotEmpty()) }
    }
}
