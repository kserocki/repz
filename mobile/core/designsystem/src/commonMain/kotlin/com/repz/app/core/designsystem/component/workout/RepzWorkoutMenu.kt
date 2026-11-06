package com.repz.app.core.designsystem.component.workout

import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.repz.app.core.designsystem.generated.resources.Res
import com.repz.app.core.designsystem.generated.resources.symbol_check
import com.repz.app.core.designsystem.theme.RepzIconSize
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.painterResource

/**
 * The dropdown menu internal to the live-workout components (header overflow, exercise-card menu,
 * set-type badge menu): DESIGN_SYSTEM §9 notes these menus are component-internal, not a generic
 * `core:designsystem` menu API. [RepzMenuItem.isDestructive] colours the row's
 * text `error`; [RepzMenuItem.isChecked] shows a trailing check mark (e.g. the current set type).
 */
@Composable
internal fun RepzWorkoutDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: ImmutableList<RepzMenuItem>,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest) {
        items.forEach { item ->
            DropdownMenuItem(
                text = { Text(item.label) },
                onClick = {
                    onDismissRequest()
                    item.onClick()
                },
                modifier = item.modifier,
                trailingIcon =
                    if (item.isChecked) {
                        { RepzWorkoutMenuCheckIcon() }
                    } else {
                        null
                    },
                colors =
                    if (item.isDestructive) {
                        MenuDefaults.itemColors(
                            textColor = MaterialTheme.colorScheme.error,
                            trailingIconColor = MaterialTheme.colorScheme.error,
                        )
                    } else {
                        MenuDefaults.itemColors()
                    },
            )
        }
    }
}

@Composable
private fun RepzWorkoutMenuCheckIcon() {
    Icon(
        painter = painterResource(Res.drawable.symbol_check),
        contentDescription = null,
        modifier = Modifier.size(RepzIconSize.dense),
    )
}
