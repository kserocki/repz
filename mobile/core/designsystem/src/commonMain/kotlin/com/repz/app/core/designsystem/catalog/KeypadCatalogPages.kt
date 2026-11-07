package com.repz.app.core.designsystem.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.repz.app.core.designsystem.format.FixedDecimal
import com.repz.app.core.designsystem.keypad.KeypadField
import com.repz.app.core.designsystem.keypad.KeypadMode
import com.repz.app.core.designsystem.keypad.KeypadQuickValues
import com.repz.app.core.designsystem.theme.RepzSpacing

// Hundredths for the Kg/Lb sample fields below (Detekt's MagicNumber doesn't exempt enum
// constructor arguments the way it exempts a plain property declaration).
private const val KG_MAX_HUNDREDTHS = 1000_00L
private const val KG_STEP_HALF_HUNDREDTHS = 50L
private const val KG_STEP_QUARTER_HUNDREDTHS = 125L
private const val KG_STEP_DEFAULT_HUNDREDTHS = 250L
private const val KG_STEP_DOUBLE_HUNDREDTHS = 500L
private const val KG_SAMPLE_HUNDREDTHS = 8250L
private const val LB_MAX_HUNDREDTHS = 220462L
private const val LB_STEP_SMALL_HUNDREDTHS = 100L
private const val LB_STEP_DEFAULT_HUNDREDTHS = 250L
private const val LB_STEP_DOUBLE_HUNDREDTHS = 500L
private const val LB_SAMPLE_HUNDREDTHS = 18500L

// Sample data only: the catalog plays the caller role with FixedDecimal directly (it can't see
// core:ui's domain-aware mapping, which SetValueKeypadMappingTest covers instead).
internal enum class CatalogWeightUnit(
    val label: String,
    val max: FixedDecimal,
    val stepOptions: List<FixedDecimal>,
    val sample: FixedDecimal,
) {
    Kg(
        label = "kg",
        max = FixedDecimal(KG_MAX_HUNDREDTHS),
        stepOptions =
            listOf(KG_STEP_HALF_HUNDREDTHS, KG_STEP_QUARTER_HUNDREDTHS, KG_STEP_DEFAULT_HUNDREDTHS, KG_STEP_DOUBLE_HUNDREDTHS)
                .map { FixedDecimal(it) },
        sample = FixedDecimal(KG_SAMPLE_HUNDREDTHS),
    ),
    Lb(
        label = "lb",
        max = FixedDecimal(LB_MAX_HUNDREDTHS),
        stepOptions =
            listOf(LB_STEP_SMALL_HUNDREDTHS, LB_STEP_DEFAULT_HUNDREDTHS, LB_STEP_DOUBLE_HUNDREDTHS).map { FixedDecimal(it) },
        sample = FixedDecimal(LB_SAMPLE_HUNDREDTHS),
    ),
}

internal const val DEFAULT_WEIGHT_STEP_INDEX = 2 // 2.5 kg / 5 lb (both units' 3rd option)
internal val SAMPLE_REPS = FixedDecimal.ofWhole(8)
private val REPS_MAX = FixedDecimal.ofWhole(999)
private val REPS_STEP = FixedDecimal.ofWhole(1)
private const val RPE_MIN_HUNDREDTHS = 600L
private const val RPE_MAX_HUNDREDTHS = 1000L
private const val RPE_STEP_HUNDREDTHS = 50L
private val RPE_CHOICES: List<FixedDecimal> =
    generateSequence(RPE_MIN_HUNDREDTHS) { it + RPE_STEP_HUNDREDTHS }
        .takeWhile { it <= RPE_MAX_HUNDREDTHS }
        .map { FixedDecimal(it) }
        .toList()
private val RPE_STEP = FixedDecimal(RPE_STEP_HUNDREDTHS)
internal val SAMPLE_RPE = FixedDecimal(800)

internal fun catalogWeightField(
    unit: CatalogWeightUnit,
    step: FixedDecimal,
    value: FixedDecimal?,
    last: FixedDecimal?,
): KeypadField {
    val base = last ?: value
    return KeypadField(
        mode = KeypadMode.Weight,
        value = value,
        min = FixedDecimal.ZERO,
        max = unit.max,
        step = step,
        fractionDigits = 2,
        sameAsLast = last,
        quickValues = KeypadQuickValues.weightSuggestions(base = base, step = step, min = FixedDecimal.ZERO, max = unit.max),
        choices = emptyList(),
    )
}

internal fun catalogRepsField(
    value: FixedDecimal?,
    last: FixedDecimal?,
): KeypadField =
    KeypadField(
        mode = KeypadMode.Reps,
        value = value,
        min = FixedDecimal.ZERO,
        max = REPS_MAX,
        step = REPS_STEP,
        fractionDigits = 0,
        sameAsLast = last,
        quickValues = KeypadQuickValues.repsSuggestions,
        choices = emptyList(),
    )

internal fun catalogRpeField(
    value: FixedDecimal?,
    last: FixedDecimal?,
): KeypadField =
    KeypadField(
        mode = KeypadMode.Rpe,
        value = value,
        min = RPE_CHOICES.first(),
        max = RPE_CHOICES.last(),
        step = RPE_STEP,
        fractionDigits = 1,
        sameAsLast = last,
        quickValues = emptyList(),
        choices = RPE_CHOICES,
    )

/** The last 3 lines a catalog page shows: the last emitted value, the last mode switch, and the
 * last Next/Dismiss/Step event (plan §3.8). */
internal class CatalogEventLog {
    var lastValue by mutableStateOf("value: (none yet)")
    var lastMode by mutableStateOf("mode: (unchanged)")
    var lastAction by mutableStateOf("action: (none yet)")

    val lines: List<String> get() = listOf(lastValue, lastMode, lastAction)
}

@Composable
internal fun rememberCatalogEventLog(): CatalogEventLog = remember { CatalogEventLog() }

@Composable
internal fun CatalogEventLogColumn(log: CatalogEventLog) {
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.xxs)) {
        CatalogSectionLabel("event log")
        for (line in log.lines) {
            Text(line, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun CatalogHapticLogColumn(entries: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.xxs)) {
        CatalogSectionLabel("last haptics")
        for (entry in entries.take(HAPTIC_LOG_DISPLAY_COUNT)) {
            Text(entry, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private const val HAPTIC_LOG_DISPLAY_COUNT = 4
