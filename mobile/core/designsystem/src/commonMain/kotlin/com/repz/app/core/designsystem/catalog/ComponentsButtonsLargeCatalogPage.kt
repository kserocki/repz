package com.repz.app.core.designsystem.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.repz.app.core.designsystem.component.RepzButton
import com.repz.app.core.designsystem.component.RepzButtonSize
import com.repz.app.core.designsystem.component.RepzConnectedButtonGroup
import com.repz.app.core.designsystem.component.RepzHeroStartButton
import com.repz.app.core.designsystem.component.RepzHeroStartSplitButton
import com.repz.app.core.designsystem.component.RepzSplitButton
import com.repz.app.core.designsystem.theme.RepzSpacing
import kotlinx.collections.immutable.persistentListOf

/** `components-buttons-large` (DESIGN_SYSTEM §9 `RepzButton`/`RepzConnectedButtonGroup`/
 * `RepzSplitButton`/`RepzHeroStartButton`): sizes L and XL, the connected button group, the split
 * button, the hero button and its split variant. */
@Composable
internal fun ComponentsButtonsLargeCatalogPage() {
    var unitIndex by remember { mutableIntStateOf(0) }
    val units = persistentListOf("kg", "lb")

    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.lg)) {
        val largeLabel = "Start workout"
        RepzButton(largeLabel, onClick = {}, size = RepzButtonSize.Large)
        RepzButton(largeLabel, onClick = {}, size = RepzButtonSize.ExtraLarge)

        RepzConnectedButtonGroup(options = units, selectedIndex = unitIndex, onSelectedIndexChange = { unitIndex = it })

        val finishLabel = "Finish"
        val finishMenuDescription = "Finish options"
        RepzSplitButton(
            text = finishLabel,
            onClick = {},
            onTrailingClick = {},
            trailingContentDescription = finishMenuDescription,
        )

        val startWorkoutLabel = "Start workout"
        RepzHeroStartButton(startWorkoutLabel, onClick = {}, modifier = Modifier.fillMaxWidth())

        val fromTemplateDescription = "Start from template"
        RepzHeroStartSplitButton(
            text = startWorkoutLabel,
            onClick = {},
            secondaryContentDescription = fromTemplateDescription,
            onSecondaryClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
