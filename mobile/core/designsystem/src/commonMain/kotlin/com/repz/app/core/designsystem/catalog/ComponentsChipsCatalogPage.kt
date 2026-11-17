package com.repz.app.core.designsystem.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.repz.app.core.designsystem.component.RepzAssistChip
import com.repz.app.core.designsystem.component.RepzConnectedButtonGroup
import com.repz.app.core.designsystem.component.RepzFilterChip
import com.repz.app.core.designsystem.component.RepzIcon
import com.repz.app.core.designsystem.component.RepzInputChip
import com.repz.app.core.designsystem.haptics.LocalRepzHaptics
import com.repz.app.core.designsystem.haptics.RepzHaptics
import com.repz.app.core.designsystem.haptics.rememberPlatformHapticPerformer
import com.repz.app.core.designsystem.theme.RepzSpacing
import kotlinx.collections.immutable.persistentListOf

/** `components-chips` (DESIGN_SYSTEM §9 `RepzFilterChip`/`RepzInputChip`/`RepzAssistChip`/
 * `RepzConnectedButtonGroup`): every chip state (unselected, selected, with/without a leading icon,
 * disabled, removable) plus the board 04 "kg / lb" connected group, with the catalog haptic log
 * proving a filter-chip toggle fires Confirm (MOTION.md §3). Tapping "Chest" or "Back" really
 * toggles; tapping the input chip's remove button really removes it.
 */
@Composable
internal fun ComponentsChipsCatalogPage() {
    val platformPerformer = rememberPlatformHapticPerformer()
    val log = remember(platformPerformer) { CatalogHapticLog(platformPerformer) }
    val loggedHaptics = remember(log) { RepzHaptics(log, enabled = true) }

    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.lg)) {
        CompositionLocalProvider(LocalRepzHaptics provides loggedHaptics) {
            FilterChipSection()
            InputChipSection()
            AssistChipSection()
            ConnectedGroupSection()
        }

        CatalogSectionLabel("event log")
        Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.xxs)) {
            log.entries.forEach { entry ->
                Text(text = entry, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun FilterChipSection() {
    var chestSelected by remember { mutableStateOf(true) }
    var backSelected by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
        CatalogSectionLabel("filter chip")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(RepzSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(RepzSpacing.sm),
        ) {
            RepzFilterChip(label = "Chest", selected = chestSelected, onSelectedChange = { chestSelected = it })
            RepzFilterChip(label = "Back", selected = backSelected, onSelectedChange = { backSelected = it })
            RepzFilterChip(
                label = "Barbell",
                selected = false,
                onSelectedChange = {},
                leadingIcon = RepzIcon.FitnessCenter,
            )
            RepzFilterChip(label = "Disabled", selected = false, onSelectedChange = {}, enabled = false)
        }
    }
}

@Composable
private fun InputChipSection() {
    var showInputChip by remember { mutableStateOf(true) }
    val removedMessage = "Removed (reopen the page to show it again)"

    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
        CatalogSectionLabel("input chip")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
            if (showInputChip) {
                RepzInputChip(label = "Lateral Raise", onRemove = { showInputChip = false })
            } else {
                Text(removedMessage, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun AssistChipSection() {
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
        CatalogSectionLabel("assist chip")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
            RepzAssistChip(label = "Same as last", onClick = {}, leadingIcon = RepzIcon.History)
            RepzAssistChip(label = "Disabled", onClick = {}, leadingIcon = RepzIcon.History, enabled = false)
        }
    }
}

@Composable
private fun ConnectedGroupSection() {
    var unitIndex by remember { mutableIntStateOf(0) }

    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
        CatalogSectionLabel("connected button group")
        RepzConnectedButtonGroup(
            options = persistentListOf("kg", "lb"),
            selectedIndex = unitIndex,
            onSelectedIndexChange = { unitIndex = it },
        )
    }
}
