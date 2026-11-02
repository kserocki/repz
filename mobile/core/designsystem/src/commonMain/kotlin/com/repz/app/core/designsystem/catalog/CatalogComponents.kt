package com.repz.app.core.designsystem.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.repz.app.core.designsystem.theme.RepzSpacing
import kotlin.math.roundToInt

private val SwatchSize = 28.dp
internal val CatalogTileSize = 56.dp

/** One row's worth of colour data for [ColorSwatchGrid]: a token name, its hex (computed from the
 * live [Color], so it reflects dynamic colour too) and the colour itself. */
internal data class ColorEntry(
    val name: String,
    val color: Color,
) {
    val hex: String = color.toHexLabel()
}

@Composable
internal fun CatalogPageTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
}

@Composable
internal fun CatalogSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/** A grid of [ColorEntry] rows arranged across [columns] columns, each row showing a small swatch,
 * the token name and its hex. Built from plain [Column]/[Row] (not `LazyVerticalGrid`) because the
 * catalog page itself already scrolls. */
@Composable
internal fun ColorSwatchGrid(
    entries: List<ColorEntry>,
    columns: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
        entries.chunked(columns).forEach { rowEntries ->
            Row(horizontalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
                rowEntries.forEach { entry -> ColorSwatchCell(entry = entry, modifier = Modifier.weight(1f)) }
                repeat(columns - rowEntries.size) { Box(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ColorSwatchCell(
    entry: ColorEntry,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(RepzSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(SwatchSize)
                    .background(entry.color, MaterialTheme.shapes.extraSmall),
        )
        Column {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = entry.hex,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

/** A chip demonstrating one extended-colour pair: the container colour as the background, the
 * on-container colour as the (AA-contrast) text. */
@Composable
internal fun SemanticChip(
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .background(containerColor, MaterialTheme.shapes.small)
                .padding(horizontal = RepzSpacing.md, vertical = RepzSpacing.sm),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = contentColor)
    }
}

/** One shape-scale or accent tile: a fixed-size coloured surface clipped to [shape], labelled
 * underneath. */
@Composable
internal fun ShapeTile(
    label: String,
    shape: Shape,
    containerColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = CatalogTileSize,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RepzSpacing.xxs),
    ) {
        Box(modifier = Modifier.size(size).clip(shape).background(containerColor))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private const val HEX_RADIX = 16

private fun Int.toHexByte(): String = toString(HEX_RADIX).padStart(2, '0').uppercase()

internal fun Color.toHexLabel(): String {
    val r = (red * 255f).roundToInt().coerceIn(0, 255)
    val g = (green * 255f).roundToInt().coerceIn(0, 255)
    val b = (blue * 255f).roundToInt().coerceIn(0, 255)
    return "#${r.toHexByte()}${g.toHexByte()}${b.toHexByte()}"
}
