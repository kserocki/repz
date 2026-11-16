package com.repz.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.designsystem.theme.RepzTheme

// Board 04's own button labels, reused here as catalog/preview sample copy only.
private fun labelFor(style: RepzButtonStyle) =
    when (style) {
        RepzButtonStyle.Filled -> "Add set"
        RepzButtonStyle.Tonal -> "Save template"
        RepzButtonStyle.Outlined -> "Keep training"
        RepzButtonStyle.Text -> "See all"
        RepzButtonStyle.Hero -> "Create account"
    }

/** One row per style, each showing small/medium, disabled and loading, so every state in
 * DESIGN_SYSTEM §9's button table is visible at once. */
@Composable
private fun RepzButtonStylesShowcase() {
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
        for (style in RepzButtonStyle.entries) {
            val label = labelFor(style)
            Row(horizontalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
                RepzButton(label, onClick = {}, style = style, size = RepzButtonSize.Small)
                RepzButton(label, onClick = {}, style = style, size = RepzButtonSize.Medium)
                RepzButton(label, onClick = {}, style = style, enabled = false)
                RepzButton(label, onClick = {}, style = style, isLoading = true)
            }
        }
    }
}

/** XS through XL at the Filled style, so every size's height, padding and label style (U6c) is
 * visible in one place. */
@Composable
private fun RepzButtonSizesShowcase() {
    val label = "Start"
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
        for (size in RepzButtonSize.entries) {
            RepzButton(label, onClick = {}, size = size)
        }
    }
}

/** Leading icon on Filled and Tonal, the two styles the live-workout "Add set"/"Add exercise"
 * actions combine it with most (DESIGN_SYSTEM §9 interfaces table). */
@Composable
private fun RepzButtonLeadingIconShowcase() {
    Row(horizontalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
        RepzButton("Add set", onClick = {}, style = RepzButtonStyle.Filled, leadingIcon = RepzIcon.Add)
        RepzButton("Add exercise", onClick = {}, style = RepzButtonStyle.Tonal, leadingIcon = RepzIcon.Add)
    }
}

@Preview
@Composable
private fun RepzButtonStylesLightPreview() {
    RepzTheme(darkTheme = false) { RepzButtonStylesShowcase() }
}

@Preview
@Composable
private fun RepzButtonStylesDarkPreview() {
    RepzTheme(darkTheme = true) { RepzButtonStylesShowcase() }
}

@Preview
@Composable
private fun RepzButtonSizesLightPreview() {
    RepzTheme(darkTheme = false) { RepzButtonSizesShowcase() }
}

@Preview
@Composable
private fun RepzButtonSizesDarkPreview() {
    RepzTheme(darkTheme = true) { RepzButtonSizesShowcase() }
}

@Preview
@Composable
private fun RepzButtonLeadingIconLightPreview() {
    RepzTheme(darkTheme = false) { RepzButtonLeadingIconShowcase() }
}

@Preview
@Composable
private fun RepzButtonLeadingIconDarkPreview() {
    RepzTheme(darkTheme = true) { RepzButtonLeadingIconShowcase() }
}
