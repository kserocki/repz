package com.repz.app.core.designsystem.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.repz.app.core.designsystem.component.RepzExerciseListItem
import com.repz.app.core.designsystem.component.RepzIcon
import com.repz.app.core.designsystem.component.RepzSearchBar
import com.repz.app.core.designsystem.theme.RepzSpacing

/** `components-search` (DESIGN_SYSTEM §9 `RepzSearchBar`/`RepzExerciseListItem`, the exercise
 * picker's C13): an empty and a typed search bar, its decorative and clickable leading-icon
 * variants, one bar forced to 200 % font, and the exercise row's plain, unselected and selected
 * states. Tapping a row or the clear icon really updates this page's own state. */
@Composable
internal fun ComponentsSearchCatalogPage() {
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.lg)) {
        SearchBarSection()
        ExerciseListItemSection()
    }
}

@Composable
private fun SearchBarSection() {
    val searchPlaceholder = "Search exercises"
    val clearDescription = "Clear search"
    val closeDescription = "Close picker"
    var emptyQuery by remember { mutableStateOf("") }
    var typedQuery by remember { mutableStateOf("Bench") }
    var closableQuery by remember { mutableStateOf("Row") }
    var scaledQuery by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
        CatalogSectionLabel("search bar")
        RepzSearchBar(
            query = emptyQuery,
            onQueryChange = { emptyQuery = it },
            placeholder = searchPlaceholder,
            clearContentDescription = clearDescription,
        )
        RepzSearchBar(
            query = typedQuery,
            onQueryChange = { typedQuery = it },
            placeholder = searchPlaceholder,
            clearContentDescription = clearDescription,
        )
        RepzSearchBar(
            query = closableQuery,
            onQueryChange = { closableQuery = it },
            placeholder = searchPlaceholder,
            clearContentDescription = clearDescription,
            leadingIcon = RepzIcon.Close,
            onLeadingClick = {},
            leadingContentDescription = closeDescription,
        )
        CompositionLocalProvider(
            LocalDensity provides Density(density = LocalDensity.current.density, fontScale = 2f),
        ) {
            RepzSearchBar(
                query = scaledQuery,
                onQueryChange = { scaledQuery = it },
                placeholder = searchPlaceholder,
                clearContentDescription = clearDescription,
            )
        }
    }
}

@Composable
private fun ExerciseListItemSection() {
    val benchName = "Bench Press"
    val benchMeta = "Chest · Barbell"
    val rowName = "Barbell Row"
    val rowMeta = "Back · Barbell"
    var isRowSelected by remember { mutableStateOf(true) }

    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
        CatalogSectionLabel("exercise list item")
        RepzExerciseListItem(name = benchName, supportingText = benchMeta, onClick = {})
        RepzExerciseListItem(name = rowName, supportingText = rowMeta, onClick = {}, isSelected = false)
        RepzExerciseListItem(
            name = rowName,
            supportingText = rowMeta,
            onClick = { isRowSelected = !isRowSelected },
            isSelected = isRowSelected,
        )
    }
}
