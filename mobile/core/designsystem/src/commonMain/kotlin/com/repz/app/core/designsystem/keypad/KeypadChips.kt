package com.repz.app.core.designsystem.keypad

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.repz.app.core.designsystem.component.RepzIcon
import com.repz.app.core.designsystem.format.FixedDecimal
import com.repz.app.core.designsystem.format.RepzNumberSymbols
import com.repz.app.core.designsystem.generated.resources.Res
import com.repz.app.core.designsystem.generated.resources.keypad_same_as_last
import com.repz.app.core.designsystem.generated.resources.keypad_step
import com.repz.app.core.designsystem.generated.resources.keypad_step_change
import com.repz.app.core.designsystem.theme.RepzSpacing
import org.jetbrains.compose.resources.stringResource

private val ChipHeight = 32.dp
private val ChipTouchTarget = 48.dp
private val ChipIconSize = 18.dp
private val ChipBorderWidth = 1.dp

/** A chip's 32 dp visual pill inside a 48 dp touch target (DESIGN_SYSTEM §9). */
@Composable
private fun KeypadChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onClickLabel: String? = null,
    containerColor: Color = Color.Transparent,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    border: BorderStroke? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    label: @Composable () -> Unit,
) {
    Box(modifier = modifier.heightIn(min = ChipTouchTarget), contentAlignment = Alignment.Center) {
        Row(
            modifier =
                Modifier
                    .height(ChipHeight)
                    .clip(MaterialTheme.shapes.small)
                    .then(if (border != null) Modifier.border(border, MaterialTheme.shapes.small) else Modifier)
                    .background(containerColor)
                    .clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
                    .padding(horizontal = RepzSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(RepzSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                leadingIcon?.invoke()
                label()
            }
        }
    }
}

/** "Same as last · {value}": `primaryContainer`/`onPrimaryContainer`, the `history` icon. Shown
 * only when the caller passes a value for the current field. */
@Composable
internal fun SameAsLastChip(
    value: FixedDecimal,
    symbols: RepzNumberSymbols,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    KeypadChip(
        onClick = onClick,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = contentColor,
        leadingIcon = {
            RepzIcon(
                icon = RepzIcon.History,
                contentDescription = null,
                modifier = Modifier.size(ChipIconSize),
                tint = contentColor,
            )
        },
        label = {
            Text(
                stringResource(Res.string.keypad_same_as_last, value.format(symbols)),
                style = MaterialTheme.typography.labelLarge,
            )
        },
    )
}

/** A plain outlined numeric suggestion (base ± steps for weight, fixed 5/6/8/10/12 for reps). */
@Composable
internal fun QuickValueChip(
    value: FixedDecimal,
    symbols: RepzNumberSymbols,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KeypadChip(
        onClick = onClick,
        modifier = modifier,
        border = BorderStroke(ChipBorderWidth, MaterialTheme.colorScheme.outlineVariant),
        label = { Text(value.format(symbols), style = MaterialTheme.typography.labelLarge) },
    )
}

/** "Step {value}": outlined, the `tune` icon; a tap cycles [NumberKeypadRequest.stepOptions].
 * Weight mode only; hidden when the caller passes no step options. */
@Composable
internal fun StepChip(
    step: FixedDecimal,
    symbols: RepzNumberSymbols,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val changeLabel = stringResource(Res.string.keypad_step_change)
    KeypadChip(
        onClick = onClick,
        modifier = modifier,
        onClickLabel = changeLabel,
        border = BorderStroke(ChipBorderWidth, MaterialTheme.colorScheme.outlineVariant),
        leadingIcon = { RepzIcon(icon = RepzIcon.Tune, contentDescription = null, modifier = Modifier.size(ChipIconSize)) },
        label = { Text(stringResource(Res.string.keypad_step, step.format(symbols)), style = MaterialTheme.typography.labelLarge) },
    )
}

/** The chip row (`FlowRow`, wraps at 200 % font): Same as last, quick values, then the Step chip
 * (weight mode only); renders nothing when none apply, so the sheet doesn't reserve empty space. */
@Composable
internal fun KeypadChipRow(
    field: KeypadField,
    showStepChip: Boolean,
    symbols: RepzNumberSymbols,
    onSetValue: (FixedDecimal) -> Unit,
    onStepClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasAnyChip = field.sameAsLast != null || field.quickValues.isNotEmpty() || showStepChip
    if (!hasAnyChip) return

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(RepzSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(RepzSpacing.sm),
    ) {
        field.sameAsLast?.let { value -> SameAsLastChip(value = value, symbols = symbols, onClick = { onSetValue(value) }) }
        for (value in field.quickValues) {
            QuickValueChip(value = value, symbols = symbols, onClick = { onSetValue(value) })
        }
        if (showStepChip) {
            StepChip(step = field.step, symbols = symbols, onClick = onStepClick)
        }
    }
}
