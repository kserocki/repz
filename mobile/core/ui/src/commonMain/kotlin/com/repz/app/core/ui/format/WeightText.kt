package com.repz.app.core.ui.format

import androidx.compose.runtime.Composable
import com.repz.app.core.designsystem.format.rememberRepzNumberSymbols
import com.repz.app.core.domain.model.UnitSystem
import com.repz.app.core.domain.model.Weight
import com.repz.app.core.ui.generated.resources.Res
import com.repz.app.core.ui.generated.resources.ui_unit_kg
import com.repz.app.core.ui.generated.resources.ui_unit_lb
import com.repz.app.core.ui.generated.resources.ui_weight_with_unit
import org.jetbrains.compose.resources.stringResource

/** "82.5" / "82,5": the locale decimal separator, no unit (SetRow, Previous column). */
@Composable
fun formatSetWeight(
    weight: Weight,
    unit: UnitSystem,
): String {
    val symbols = rememberRepzNumberSymbols()
    return WeightFormat.toDisplay(weight, unit).format(symbols)
}

/** "82.5 kg": [formatSetWeight] followed by [weightUnitLabel], joined with a non-breaking space. */
@Composable
fun formatSetWeightWithUnit(
    weight: Weight,
    unit: UnitSystem,
): String = stringResource(Res.string.ui_weight_with_unit, formatSetWeight(weight, unit), weightUnitLabel(unit))

/** "kg" / "lb", localized. */
@Composable
fun weightUnitLabel(unit: UnitSystem): String =
    when (unit) {
        UnitSystem.KG -> stringResource(Res.string.ui_unit_kg)
        UnitSystem.LB -> stringResource(Res.string.ui_unit_lb)
    }
