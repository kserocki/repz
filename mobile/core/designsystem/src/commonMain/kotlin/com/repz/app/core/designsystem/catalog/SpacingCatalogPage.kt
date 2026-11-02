package com.repz.app.core.designsystem.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.repz.app.core.designsystem.theme.RepzSpacing

private val SpacingBarHeight = 8.dp

/** Part 1 of 2 of DESIGN_SYSTEM §6–7: the 9 [RepzSpacing] tokens as bars (length = token). Part 2
 * is [ElevationCatalogPage]. */
@Composable
internal fun SpacingCatalogPage() {
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
                    .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.extraSmall),
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
