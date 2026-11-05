package com.repz.app.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.repz.app.core.designsystem.theme.RepzIconSize
import com.repz.app.core.designsystem.theme.RepzSpacing

// Board 13 `.li`: the icon-text gap is an exact board measurement, kept exact as a
// component-internal size rather than rounded to a value on the §6 spacing grid.
private val ListItemMinHeight = 56.dp
private val IconTextGap = 14.dp

/**
 * A single settings-style row (DESIGN_SYSTEM §9/§6, board 13): 56 dp minimum height, 16 dp
 * (`RepzSpacing.lg`) horizontal padding, [leadingIcon] at [RepzIconSize.default], [headline] in
 * `bodyLarge`. The full row is clickable with the standard ripple and `Role.Button` semantics;
 * [headline] always wraps onto more lines, never truncates.
 */
@Composable
fun RepzListItem(
    headline: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: RepzIcon? = null,
    tone: RepzListItemTone = RepzListItemTone.Default,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = ListItemMinHeight)
                .clickable(onClick = onClick, role = Role.Button)
                .padding(horizontal = RepzSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            RepzIcon(
                icon = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(RepzIconSize.default),
                tint = tone.iconColor(),
            )
            Spacer(modifier = Modifier.width(IconTextGap))
        }
        Text(headline, style = MaterialTheme.typography.bodyLarge, color = tone.headlineColor())
    }
}

@Composable
private fun RepzListItemTone.iconColor(): Color =
    when (this) {
        RepzListItemTone.Default -> MaterialTheme.colorScheme.onSurfaceVariant
        RepzListItemTone.Destructive -> MaterialTheme.colorScheme.error
    }

@Composable
private fun RepzListItemTone.headlineColor(): Color =
    when (this) {
        RepzListItemTone.Default -> MaterialTheme.colorScheme.onSurface
        RepzListItemTone.Destructive -> MaterialTheme.colorScheme.error
    }
