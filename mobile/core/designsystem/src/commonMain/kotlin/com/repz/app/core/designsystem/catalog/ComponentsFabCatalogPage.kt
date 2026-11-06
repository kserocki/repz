package com.repz.app.core.designsystem.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.repz.app.core.designsystem.component.RepzExtendedFab
import com.repz.app.core.designsystem.component.RepzFab
import com.repz.app.core.designsystem.component.RepzFabMenu
import com.repz.app.core.designsystem.component.RepzFabMenuItem
import com.repz.app.core.designsystem.component.RepzIcon
import com.repz.app.core.designsystem.theme.RepzSpacing
import kotlinx.collections.immutable.persistentListOf

// Board 04's own FAB/FAB-menu copy, reused here as catalog sample copy only.
private const val NEW_WORKOUT_LABEL = "New workout"
private const val START_EMPTY_LABEL = "Start empty"
private const val FROM_TEMPLATE_LABEL = "From template"
private const val AUTO_MENU_INTERVAL_MILLIS = 2500L

/** `components-fab` (DESIGN_SYSTEM §9 `RepzFab`/`RepzExtendedFab`/`RepzFabMenu`): the FAB, the
 * extended FAB, a tappable FAB menu and a second menu that auto-toggles every 2.5 s, since iOS has
 * no on-device tap tool here (plan §5). */
@Composable
internal fun ComponentsFabCatalogPage() {
    var tappableMenuExpanded by remember { mutableStateOf(false) }
    val autoMenuExpanded = rememberAutoPlayState(AUTO_MENU_INTERVAL_MILLIS)
    val menuItems =
        remember {
            persistentListOf(
                RepzFabMenuItem(text = START_EMPTY_LABEL, icon = RepzIcon.Add, onClick = {}),
                RepzFabMenuItem(text = FROM_TEMPLATE_LABEL, icon = RepzIcon.ListAlt, onClick = {}),
            )
        }

    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.lg)) {
        CatalogSectionLabel("RepzFab / RepzExtendedFab")
        Row(horizontalArrangement = Arrangement.spacedBy(RepzSpacing.lg)) {
            RepzFab(icon = RepzIcon.Add, contentDescription = NEW_WORKOUT_LABEL, onClick = {})
            RepzExtendedFab(text = NEW_WORKOUT_LABEL, icon = RepzIcon.Add, onClick = {})
        }

        CatalogSectionLabel("RepzFabMenu (tap to toggle)")
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            RepzFabMenu(
                expanded = tappableMenuExpanded,
                onExpandedChange = { tappableMenuExpanded = it },
                collapsedContentDescription = NEW_WORKOUT_LABEL,
                items = menuItems,
            )
        }

        CatalogSectionLabel("RepzFabMenu (auto-toggles every 2.5 s)")
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            RepzFabMenu(
                expanded = autoMenuExpanded.value,
                onExpandedChange = { autoMenuExpanded.value = it },
                collapsedContentDescription = NEW_WORKOUT_LABEL,
                items = menuItems,
            )
        }
    }
}
