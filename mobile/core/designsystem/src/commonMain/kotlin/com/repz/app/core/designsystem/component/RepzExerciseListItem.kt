package com.repz.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.repz.app.core.designsystem.theme.RepzIconSize
import com.repz.app.core.designsystem.theme.RepzLayout
import com.repz.app.core.designsystem.theme.RepzShapes
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.designsystem.theme.RepzTheme

// Board 10 C13: the selected disc's Check is 28 dp, not RepzIconSize's 24 dp default. The
// thumbnail and the selected disc share one 48 dp footprint (RepzSpacing.xxxl) so a row's layout
// never shifts when selection toggles.
private val SelectedCheckIconSize = 28.dp

/**
 * One exercise row (DESIGN_SYSTEM §9 `ExerciseListItem`, board 10 C13): the exercise picker's and
 * library's shared building block. A 72 dp-minimum two-line row with a 48 dp leading thumbnail,
 * [name] in `titleSmall` and [supportingText] in `bodySmall`; both wrap rather than truncate, so
 * the row only grows (+30 %/PL, 200 % font).
 *
 * [isSelected] `null` renders the plain row (`Role.Button`): a transparent container and a
 * `surfaceContainerHigh` thumbnail box tinted with `RepzTheme.colors.musclePrimary`'s
 * [RepzIcon.FitnessCenter]. A non-null value renders the selectable variant (`toggleable`,
 * `Role.Checkbox`): unselected looks the same as the plain row; selected swaps in a
 * `secondaryContainer` background, `onSecondaryContainer` text and a `primary` disc with a
 * [RepzIcon.Check] in place of the thumbnail. That swap is instant here.
 * TODO(REP-46): the row's own Circle -> Check disc morph. The thumbnail is purely decorative; the
 * row's own merged semantics already read [name] and [supportingText] together with the checked
 * state.
 */
@Composable
fun RepzExerciseListItem(
    name: String,
    supportingText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean? = null,
) {
    val isSelectable = isSelected != null
    val containerColor = if (isSelected == true) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    val contentColor = if (isSelected == true) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
    val supportingColor =
        if (isSelected == true) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val selectionModifier =
        if (isSelectable) {
            Modifier.toggleable(value = isSelected == true, role = Role.Checkbox, onValueChange = { onClick() })
        } else {
            Modifier.clickable(onClick = onClick, role = Role.Button)
        }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = RepzLayout.listItemTwoLineHeight)
                .clip(MaterialTheme.shapes.large)
                .background(containerColor)
                .then(selectionModifier)
                .padding(horizontal = RepzSpacing.md, vertical = RepzSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RepzSpacing.md),
    ) {
        ExerciseListItemThumbnail(isSelected = isSelected == true)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, style = MaterialTheme.typography.titleSmall, color = contentColor)
            Text(text = supportingText, style = MaterialTheme.typography.bodySmall, color = supportingColor)
        }
    }
}

@Composable
private fun ExerciseListItemThumbnail(isSelected: Boolean) {
    if (isSelected) {
        Box(
            modifier =
                Modifier
                    .size(RepzSpacing.xxxl)
                    .clip(RepzShapes.full)
                    .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            RepzIcon(
                icon = RepzIcon.Check,
                contentDescription = null,
                modifier = Modifier.size(SelectedCheckIconSize),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    } else {
        Box(
            modifier =
                Modifier
                    .size(RepzSpacing.xxxl)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            RepzIcon(
                icon = RepzIcon.FitnessCenter,
                contentDescription = null,
                modifier = Modifier.size(RepzIconSize.default),
                tint = RepzTheme.colors.musclePrimary,
            )
        }
    }
}
