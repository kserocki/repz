package com.repz.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.designsystem.theme.RepzTheme
import kotlinx.collections.immutable.persistentListOf

// Board 04's own FAB menu copy, reused here as preview sample copy only.
private const val NEW_WORKOUT_LABEL = "New workout"
private const val START_EMPTY_LABEL = "Start empty"
private const val FROM_TEMPLATE_LABEL = "From template"

@Composable
private fun sampleItems() =
    remember {
        persistentListOf(
            RepzFabMenuItem(text = START_EMPTY_LABEL, icon = RepzIcon.Add, onClick = {}),
            RepzFabMenuItem(text = FROM_TEMPLATE_LABEL, icon = RepzIcon.ListAlt, onClick = {}),
        )
    }

/** Collapsed and expanded side by side, so every [RepzFabMenu] state is visible at once. */
@Composable
private fun RepzFabMenuShowcase() {
    val items = sampleItems()
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(RepzSpacing.xl),
    ) {
        RepzFabMenu(expanded = false, onExpandedChange = {}, collapsedContentDescription = NEW_WORKOUT_LABEL, items = items)
        RepzFabMenu(expanded = true, onExpandedChange = {}, collapsedContentDescription = NEW_WORKOUT_LABEL, items = items)
    }
}

@Preview
@Composable
private fun RepzFabMenuLightPreview() {
    RepzTheme(darkTheme = false) { RepzFabMenuShowcase() }
}

@Preview
@Composable
private fun RepzFabMenuDarkPreview() {
    RepzTheme(darkTheme = true) { RepzFabMenuShowcase() }
}

/** MOT-30(e): shapes/rotation jump, items get the 150 ms crossfade instead of rising and scaling. */
@Preview
@Composable
private fun RepzFabMenuReducedMotionPreview() {
    RepzTheme(darkTheme = false, reducedMotion = true) { RepzFabMenuShowcase() }
}
