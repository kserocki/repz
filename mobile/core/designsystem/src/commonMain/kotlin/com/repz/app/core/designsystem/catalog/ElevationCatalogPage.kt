package com.repz.app.core.designsystem.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.unit.dp
import com.repz.app.core.designsystem.theme.RepzElevation
import com.repz.app.core.designsystem.theme.RepzElevationLevel
import com.repz.app.core.designsystem.theme.RepzSpacing

private val ElevationCardSize = 56.dp
private val Level0OutlineWidth = 1.dp

/** Part 2 of 2 of DESIGN_SYSTEM §6–7: the 6 [RepzElevation] levels as container-coloured cards with
 * their shadow, if any. `level0`'s container equals the page background, so it gets a thin outline
 * to stay visible. Part 1 is [SpacingCatalogPage]. */
@Composable
internal fun ElevationCatalogPage() {
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
        ElevationRow("level0", RepzElevation.level0, outlined = true)
        ElevationRow("level1", RepzElevation.level1)
        ElevationRow("level2", RepzElevation.level2)
        ElevationRow("level3", RepzElevation.level3)
        ElevationRow("level4", RepzElevation.level4)
        ElevationRow("fab", RepzElevation.fab)
    }
}

@Composable
private fun ElevationRow(
    label: String,
    level: RepzElevationLevel,
    outlined: Boolean = false,
) {
    val shape = MaterialTheme.shapes.medium
    val shadow = level.shadow
    val caption = elevationCaption(label, hasShadow = shadow != null)
    Row(horizontalArrangement = Arrangement.spacedBy(RepzSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier =
                Modifier
                    .size(ElevationCardSize)
                    .let { if (shadow != null) it.dropShadow(shape, shadow) else it }
                    .background(level.containerColor, shape)
                    .let {
                        if (outlined) {
                            it.border(Level0OutlineWidth, MaterialTheme.colorScheme.outlineVariant, shape)
                        } else {
                            it
                        }
                    },
        )
        Text(
            text = caption,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun elevationCaption(
    label: String,
    hasShadow: Boolean,
): String = if (hasShadow) "$label (shadow)" else label
