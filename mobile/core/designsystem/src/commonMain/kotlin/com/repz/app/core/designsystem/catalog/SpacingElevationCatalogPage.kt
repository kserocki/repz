package com.repz.app.core.designsystem.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.repz.app.core.designsystem.theme.RepzElevation
import com.repz.app.core.designsystem.theme.RepzElevationLevel
import com.repz.app.core.designsystem.theme.RepzSpacing

private val SpacingBarHeight = 8.dp
private val ElevationCardSize = 56.dp
private val ElevationCardShape = RoundedCornerShape(RepzSpacing.md)

/** The 9 [RepzSpacing] tokens as bars (length = token), and the 6 [RepzElevation] levels as
 * container-coloured cards with their shadow, if any (DESIGN_SYSTEM §6–7). */
@Composable
internal fun SpacingElevationCatalogPage() {
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.lg)) {
        CatalogSectionLabel("spacing (9)")
        Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.xs)) {
            SpacingBar("xxs", RepzSpacing.xxs)
            SpacingBar("xs", RepzSpacing.xs)
            SpacingBar("sm", RepzSpacing.sm)
            SpacingBar("md", RepzSpacing.md)
            SpacingBar("lg", RepzSpacing.lg)
            SpacingBar("xl", RepzSpacing.xl)
            SpacingBar("xxl", RepzSpacing.xxl)
            SpacingBar("xxxl", RepzSpacing.xxxl)
            SpacingBar("huge", RepzSpacing.huge)
        }

        CatalogSectionLabel("elevation (level0–4, fab)")
        Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
            ElevationRow("level0", RepzElevation.level0)
            ElevationRow("level1", RepzElevation.level1)
            ElevationRow("level2", RepzElevation.level2)
            ElevationRow("level3", RepzElevation.level3)
            ElevationRow("level4", RepzElevation.level4)
            ElevationRow("fab", RepzElevation.fab)
        }
    }
}

@Composable
private fun SpacingBar(
    label: String,
    length: Dp,
) {
    val caption = spacingCaption(label, length)
    Row(horizontalArrangement = Arrangement.spacedBy(RepzSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier =
                Modifier
                    .width(length)
                    .height(SpacingBarHeight)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(RepzSpacing.xxs)),
        )
        Text(
            text = caption,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun spacingCaption(
    label: String,
    length: Dp,
): String = "$label ${length.value.toInt()}dp"

@Composable
private fun ElevationRow(
    label: String,
    level: RepzElevationLevel,
) {
    val shadow = level.shadow
    val caption = elevationCaption(label, hasShadow = shadow != null)
    Row(horizontalArrangement = Arrangement.spacedBy(RepzSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier =
                Modifier
                    .size(ElevationCardSize)
                    .let { if (shadow != null) it.dropShadow(ElevationCardShape, shadow) else it }
                    .background(level.containerColor, ElevationCardShape),
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
