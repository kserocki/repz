package com.repz.app.core.designsystem.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import com.repz.app.core.designsystem.motion.RepzMorphs
import com.repz.app.core.designsystem.motion.RepzMotion
import com.repz.app.core.designsystem.theme.RepzSpacing

// Q1 (set checkbox) and Q2 (FAB rest radius), both at the catalog's CatalogTileSize demo size.
private val CheckboxCornerRadius = 10.dp
private val FabCornerRadius = 16.dp

/** `motion-morphs` (DESIGN_SYSTEM §5): the 7 morph pairs, each looping with `pressMorph` except
 * `circleToBurst`, which uses `celebrate` (board 15) — fitting one iPhone Duo screen. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun MotionMorphsCatalogPage() {
    val checkboxMorph = remember { RepzMorphs.circleToRoundedSquare(CheckboxCornerRadius, CatalogTileSize) }
    val fabMorph = remember { RepzMorphs.roundedSquareToCookie4Sided(FabCornerRadius, CatalogTileSize) }

    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.md)) {
        ReducedMotionHeader()
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(RepzSpacing.md),
            verticalArrangement = Arrangement.spacedBy(RepzSpacing.md),
        ) {
            MorphTile("circleToBurst", RepzMorphs.circleToBurst, RepzMotion.springs.celebrate())
            MorphTile("sunnyToCircle", RepzMorphs.sunnyToCircle, RepzMotion.springs.pressMorph())
            MorphTile("circleToGem", RepzMorphs.circleToGem, RepzMotion.springs.pressMorph())
            MorphTile("circleToFlower", RepzMorphs.circleToFlower, RepzMotion.springs.pressMorph())
            MorphTile("flowerToCookie9Sided", RepzMorphs.flowerToCookie9Sided, RepzMotion.springs.pressMorph())
            MorphTile("circleToRoundedSquare(10)", checkboxMorph, RepzMotion.springs.pressMorph())
            MorphTile("roundedSquareToCookie4(16)", fabMorph, RepzMotion.springs.pressMorph())
        }
    }
}
