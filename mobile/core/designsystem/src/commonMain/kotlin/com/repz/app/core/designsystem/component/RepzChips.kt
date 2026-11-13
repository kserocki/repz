package com.repz.app.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.repz.app.core.designsystem.generated.resources.Res
import com.repz.app.core.designsystem.generated.resources.designsystem_remove_item
import com.repz.app.core.designsystem.haptics.RepzHaptics
import com.repz.app.core.designsystem.theme.RepzShapes
import com.repz.app.core.designsystem.theme.RepzSpacing
import org.jetbrains.compose.resources.stringResource

// Board 04 §17: every chip's icon is 18 dp (M3's own Filter/Assist chip icon size already), the
// input chip's own remove affordance is 24 dp, and the icon-to-label gap is 6 dp (off the 4 dp grid,
// a component-internal exact value, Q12 precedent).
private val ChipIconSize = 18.dp
private val ChipCloseButtonSize = 24.dp
private val ChipIconLabelGap = 6.dp
private val ChipLeadingIconPadding = PaddingValues(start = 8.dp, end = 12.dp)
private val ChipSymmetricPadding = PaddingValues(horizontal = 12.dp)
private val InputChipHeight = 32.dp
private val InputChipStartPadding = 12.dp
private val InputChipEndPadding = 4.dp

// DESIGN_SYSTEM §9 "disabled onSurface 12 % container / 38 % content" (board 04), the same rule
// RepzButton applies.
private const val DISABLED_CONTAINER_ALPHA = 0.12f
private const val DISABLED_CONTENT_ALPHA = 0.38f

/**
 * A selectable filter chip (DESIGN_SYSTEM §9, board 04 §17): 32 dp tall, label in `labelLarge`
 * (M3's own filter-chip default). Unselected: `shapes.small` (8), 1 dp `outline` border,
 * `onSurfaceVariant` label/icon; a caller's [leadingIcon] shows at the chip's own 18 dp. Selected:
 * [RepzShapes.full] pill, no border, `secondaryContainer`/`onSecondaryContainer`, a leading
 * [RepzIcon.Check] replaces [leadingIcon]. The shape swap on selection is instant: no motion is
 * specified for it. Toggling always fires [RepzHaptics.confirm] (MOTION.md §3 "chip toggle").
 */
@Composable
fun RepzFilterChip(
    label: String,
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: RepzIcon? = null,
    enabled: Boolean = true,
) {
    val haptics = RepzHaptics.current
    val hasLeadingIcon = selected || leadingIcon != null
    val resolvedLeadingIcon: (@Composable () -> Unit)? =
        when {
            selected -> {
                { RepzIcon(icon = RepzIcon.Check, contentDescription = null, modifier = Modifier.size(ChipIconSize)) }
            }

            leadingIcon != null -> {
                { RepzIcon(icon = leadingIcon, contentDescription = null, modifier = Modifier.size(ChipIconSize)) }
            }

            else -> {
                null
            }
        }

    FilterChip(
        selected = selected,
        onClick = {
            onSelectedChange(!selected)
            haptics.confirm()
        },
        label = { Text(label) },
        modifier = modifier,
        enabled = enabled,
        leadingIcon = resolvedLeadingIcon,
        shape = if (selected) RepzShapes.full else MaterialTheme.shapes.small,
        colors =
            FilterChipDefaults.filterChipColors(
                containerColor = Color.Transparent,
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledContainerColor = Color.Transparent,
                disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_CONTENT_ALPHA),
                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_CONTENT_ALPHA),
                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                disabledSelectedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_CONTAINER_ALPHA),
                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
        elevation = null,
        border = unselectedChipBorder(enabled = enabled, visible = !selected),
        horizontalArrangement = FilterChipDefaults.horizontalArrangement(spacing = ChipIconLabelGap),
        contentPadding = if (hasLeadingIcon) ChipLeadingIconPadding else ChipSymmetricPadding,
    )
}

/**
 * A clickable suggestion chip (DESIGN_SYSTEM §9, board 04 §17 "Same as last"): 32 dp, `shapes.small`,
 * 1 dp `outlineVariant`, `onSurface` label in `labelLarge`, a leading [leadingIcon] (if any) tinted
 * `primary` — all M3's own assist-chip defaults already match the board; only the content padding is
 * overridden to the board's 8/12 split when an icon is shown.
 */
@Composable
fun RepzAssistChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: RepzIcon? = null,
    enabled: Boolean = true,
) {
    val resolvedLeadingIcon: (@Composable () -> Unit)? =
        if (leadingIcon == null) {
            null
        } else {
            { RepzIcon(icon = leadingIcon, contentDescription = null, modifier = Modifier.size(ChipIconSize)) }
        }

    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier,
        enabled = enabled,
        leadingIcon = resolvedLeadingIcon,
        contentPadding = if (leadingIcon != null) ChipLeadingIconPadding else ChipSymmetricPadding,
    )
}

/**
 * A removable token chip (DESIGN_SYSTEM §9, board 04 §17): 32 dp, `shapes.small`,
 * `surfaceContainerHigh`, `onSurface` label in `labelLarge`, a 24 dp trailing remove button (18 dp
 * [RepzIcon.Close], `onSurfaceVariant`) whose content description reads "Remove [label]"
 * (`designsystem_remove_item`). Unlike [RepzFilterChip]/[RepzAssistChip] this isn't built on an M3
 * chip: the row itself never responds to a tap, only the remove button does, so it never reports a
 * toggle or button role it doesn't act on.
 */
@Composable
fun RepzInputChip(
    label: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val contentColor =
        if (enabled) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_CONTENT_ALPHA)
        }
    val iconColor =
        if (enabled) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_CONTENT_ALPHA)
        }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier =
                Modifier
                    .heightIn(min = InputChipHeight)
                    .padding(start = InputChipStartPadding, end = InputChipEndPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RepzSpacing.xs),
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = contentColor)
            // The 24 dp visual size is smaller than the touch target: IconButton's own
            // minimumInteractiveComponentSize() still expands the tappable area to 48 dp around it.
            IconButton(onClick = onRemove, enabled = enabled, modifier = Modifier.size(ChipCloseButtonSize)) {
                RepzIcon(
                    icon = RepzIcon.Close,
                    contentDescription = stringResource(Res.string.designsystem_remove_item, label),
                    modifier = Modifier.size(ChipIconSize),
                    tint = iconColor,
                )
            }
        }
    }
}

@Composable
private fun unselectedChipBorder(
    enabled: Boolean,
    visible: Boolean,
): BorderStroke? {
    if (!visible) return null
    val color =
        if (enabled) {
            MaterialTheme.colorScheme.outline
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_CONTAINER_ALPHA)
        }
    return BorderStroke(1.dp, color)
}
