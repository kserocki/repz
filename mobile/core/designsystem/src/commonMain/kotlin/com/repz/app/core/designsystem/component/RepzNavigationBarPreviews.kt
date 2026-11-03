package com.repz.app.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.generated.resources.Res
import com.repz.app.core.designsystem.generated.resources.symbol_exercise
import com.repz.app.core.designsystem.generated.resources.symbol_exercise_filled
import com.repz.app.core.designsystem.generated.resources.symbol_fitness_center
import com.repz.app.core.designsystem.generated.resources.symbol_fitness_center_filled
import com.repz.app.core.designsystem.generated.resources.symbol_history
import com.repz.app.core.designsystem.generated.resources.symbol_history_filled
import com.repz.app.core.designsystem.generated.resources.symbol_person
import com.repz.app.core.designsystem.generated.resources.symbol_person_filled
import com.repz.app.core.designsystem.theme.RepzTheme
import org.jetbrains.compose.resources.painterResource

/** One bar per tab, each with that tab selected, so every item's selected state is visible. */
@Composable
private fun RepzNavigationBarShowcase() {
    val items =
        listOf(
            Triple("Train", Res.drawable.symbol_fitness_center, Res.drawable.symbol_fitness_center_filled),
            Triple("History", Res.drawable.symbol_history, Res.drawable.symbol_history_filled),
            Triple("Exercises", Res.drawable.symbol_exercise, Res.drawable.symbol_exercise_filled),
            Triple("Profile", Res.drawable.symbol_person, Res.drawable.symbol_person_filled),
        )
    Column {
        items.forEachIndexed { selectedIndex, _ ->
            RepzNavigationBar {
                items.forEachIndexed { index, (label, icon, selectedIcon) ->
                    RepzNavigationBarItem(
                        selected = index == selectedIndex,
                        onClick = {},
                        icon = painterResource(icon),
                        selectedIcon = painterResource(selectedIcon),
                        label = label,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun RepzNavigationBarLightPreview() {
    RepzTheme(darkTheme = false) { RepzNavigationBarShowcase() }
}

@Preview
@Composable
private fun RepzNavigationBarDarkPreview() {
    RepzTheme(darkTheme = true) { RepzNavigationBarShowcase() }
}
