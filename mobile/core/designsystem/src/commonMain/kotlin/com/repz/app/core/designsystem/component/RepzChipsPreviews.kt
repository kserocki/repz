package com.repz.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.designsystem.theme.RepzTheme

@Composable
private fun RepzChipsShowcase() {
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.md)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(RepzSpacing.sm), verticalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
            RepzFilterChip(label = "Chest", selected = true, onSelectedChange = {})
            RepzFilterChip(label = "Back", selected = false, onSelectedChange = {})
            RepzFilterChip(label = "Barbell", selected = false, onSelectedChange = {}, leadingIcon = RepzIcon.FitnessCenter)
            RepzFilterChip(label = "Disabled", selected = false, onSelectedChange = {}, enabled = false)
            RepzFilterChip(label = "Disabled", selected = true, onSelectedChange = {}, enabled = false)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(RepzSpacing.sm), verticalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
            RepzAssistChip(label = "Same as last", onClick = {}, leadingIcon = RepzIcon.History)
            RepzAssistChip(label = "Repeat", onClick = {})
            RepzAssistChip(label = "Disabled", onClick = {}, leadingIcon = RepzIcon.History, enabled = false)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(RepzSpacing.sm), verticalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
            RepzInputChip(label = "Lateral Raise", onRemove = {})
            RepzInputChip(label = "Disabled", onRemove = {}, enabled = false)
        }
    }
}

// A long PL label on every chip kind: none of them truncate, they just grow (AGENTS.md §4).
@Composable
private fun RepzChipsLongLabelShowcase() {
    val longLabel = "Podnoszenie sztangi ze stania przy ścianie"
    FlowRow(horizontalArrangement = Arrangement.spacedBy(RepzSpacing.sm), verticalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
        RepzFilterChip(label = longLabel, selected = true, onSelectedChange = {})
        RepzAssistChip(label = longLabel, onClick = {}, leadingIcon = RepzIcon.History)
        RepzInputChip(label = longLabel, onRemove = {})
    }
}

@Preview
@Composable
private fun RepzChipsLightPreview() {
    RepzTheme(darkTheme = false) { RepzChipsShowcase() }
}

@Preview
@Composable
private fun RepzChipsDarkPreview() {
    RepzTheme(darkTheme = true) { RepzChipsShowcase() }
}

@Preview
@Composable
private fun RepzChipsLongLabelLightPreview() {
    RepzTheme(darkTheme = false) { RepzChipsLongLabelShowcase() }
}

@Preview
@Composable
private fun RepzChipsLongLabelDarkPreview() {
    RepzTheme(darkTheme = true) { RepzChipsLongLabelShowcase() }
}
