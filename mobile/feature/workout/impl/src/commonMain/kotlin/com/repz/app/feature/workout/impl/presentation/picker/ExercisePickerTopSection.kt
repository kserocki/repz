package com.repz.app.feature.workout.impl.presentation.picker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.repz.app.core.designsystem.component.RepzFilterChip
import com.repz.app.core.designsystem.component.RepzIcon
import com.repz.app.core.designsystem.component.RepzSearchBar
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.domain.model.Equipment
import com.repz.app.core.domain.model.Muscle
import com.repz.app.core.ui.label.label
import com.repz.app.feature.workout.impl.generated.resources.Res
import com.repz.app.feature.workout.impl.generated.resources.picker_clear_search
import com.repz.app.feature.workout.impl.generated.resources.picker_close
import com.repz.app.feature.workout.impl.generated.resources.picker_search_placeholder
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringResource

/**
 * The picker's top block (DESIGN_SYSTEM §9 picker composition, board 10 C13): [RepzSearchBar] with
 * a leading Close, then a [LazyRow] of muscle then equipment filter chips (enum order, never
 * reordering on selection) that bleeds to the screen edges — the row carries its own horizontal
 * inset as `contentPadding` instead of the column's.
 */
@Composable
internal fun ExercisePickerTopSection(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onCloseClick: () -> Unit,
    muscleChips: ImmutableList<MuscleChipUi>,
    equipmentChips: ImmutableList<EquipmentChipUi>,
    onMuscleToggle: (Muscle) -> Unit,
    onEquipmentToggle: (Equipment) -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusBarInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Column(
        modifier = modifier.padding(top = RepzSpacing.xs + statusBarInset, bottom = RepzSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(RepzSpacing.sm),
    ) {
        RepzSearchBar(
            query = query,
            onQueryChange = onQueryChange,
            placeholder = stringResource(Res.string.picker_search_placeholder),
            clearContentDescription = stringResource(Res.string.picker_clear_search),
            leadingIcon = RepzIcon.Close,
            onLeadingClick = onCloseClick,
            leadingContentDescription = stringResource(Res.string.picker_close),
            onSearch = onSearch,
            modifier = Modifier.fillMaxWidth().padding(horizontal = RepzSpacing.md),
            fieldModifier = Modifier.testTag("picker_search_field"),
            leadingModifier = Modifier.testTag("picker_close"),
            trailingModifier = Modifier.testTag("picker_clear_search"),
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = RepzSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(RepzSpacing.sm),
        ) {
            items(items = muscleChips, key = { "muscle_${it.muscle.name}" }) { chip ->
                RepzFilterChip(
                    label = chip.muscle.label(),
                    selected = chip.isSelected,
                    onSelectedChange = { onMuscleToggle(chip.muscle) },
                    modifier = Modifier.testTag("picker_chip_muscle_${chip.muscle.name}"),
                )
            }
            items(items = equipmentChips, key = { "equipment_${it.equipment.name}" }) { chip ->
                RepzFilterChip(
                    label = chip.equipment.label(),
                    selected = chip.isSelected,
                    onSelectedChange = { onEquipmentToggle(chip.equipment) },
                    modifier = Modifier.testTag("picker_chip_equipment_${chip.equipment.name}"),
                )
            }
        }
    }
}
