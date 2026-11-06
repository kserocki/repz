package com.repz.app.core.designsystem.keypad

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.repz.app.core.designsystem.component.RepzIcon
import com.repz.app.core.designsystem.format.FixedDecimal
import com.repz.app.core.designsystem.format.RepzNumberSymbols
import com.repz.app.core.designsystem.motion.RepzMotion
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.designsystem.theme.RepzTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

// MOT-10: key press RoundedRect(16) -> (28); the ± buttons share the same morph from their own
// rest radius (`shapes.largeIncreased`, DESIGN_SYSTEM §9).
internal val KeypadKeySize = 52.dp
internal val KeypadGridGap = RepzSpacing.sm
internal val KeypadNextColumnWidth = 80.dp
private val KeyRestCornerRadius = 16.dp
private val KeyPressedCornerRadius = 28.dp
private val PlusMinusRestCornerRadius = 20.dp
private val PlusMinusSize = 60.dp

// MOT-10 long-press repeat: 250 ms until 1 s held, then 60 ms.
private val InitialRepeatDelay = 250.milliseconds
private val FastRepeatInterval = 60.milliseconds
private val RepeatAccelerationThreshold = 1.seconds

private const val DISABLED_KEY_ALPHA = 0.38f

/**
 * One key of the grid: rest `surfaceContainerHighest`/`onSurface` unless overridden, pressed state
 * morphs its corner radius [KeyRestCornerRadius] -> [KeyPressedCornerRadius] on `pressMorph` and
 * flashes `primaryContainer`/`onPrimaryContainer` on `effectsFast` (MOT-10 U4a); both fall back
 * automatically under reduced motion (radius snaps, flash becomes a 150 ms crossfade), since both
 * tokens already encode that behaviour. [onClickLabel] feeds the key's accessible action name.
 */
@Composable
internal fun KeypadKeyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    restContainerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    restContentColor: Color = MaterialTheme.colorScheme.onSurface,
    onClickLabel: String? = null,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val morphed = isPressed && enabled

    val cornerRadius by
        animateDpAsState(
            targetValue = if (morphed) KeyPressedCornerRadius else KeyRestCornerRadius,
            animationSpec = RepzMotion.springs.pressMorph(),
        )
    val containerColor by
        animateColorAsState(
            targetValue = if (morphed) MaterialTheme.colorScheme.primaryContainer else restContainerColor,
            animationSpec = RepzMotion.effectsSprings.effectsFast(),
        )
    val contentColor by
        animateColorAsState(
            targetValue = if (morphed) MaterialTheme.colorScheme.onPrimaryContainer else restContentColor,
            animationSpec = RepzMotion.effectsSprings.effectsFast(),
        )

    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(cornerRadius))
                .background(containerColor)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(),
                    enabled = enabled,
                    onClickLabel = onClickLabel,
                    role = Role.Button,
                    onClick = onClick,
                ).alpha(if (enabled) 1f else DISABLED_KEY_ALPHA),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor, content = content)
    }
}

@Composable
internal fun DigitKey(
    digit: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KeypadKeyButton(onClick = onClick, modifier = modifier) {
        Text(digit.toString(), style = RepzTheme.typography.numericMedium)
    }
}

@Composable
internal fun DecimalKey(
    separator: Char,
    enabled: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KeypadKeyButton(
        onClick = onClick,
        enabled = enabled,
        onClickLabel = contentDescription,
        modifier = modifier.semantics { this.contentDescription = contentDescription },
    ) {
        Text(separator.toString(), style = RepzTheme.typography.numericMedium)
    }
}

@Composable
internal fun BackspaceKey(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KeypadKeyButton(
        onClick = onClick,
        restContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        restContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier,
    ) {
        RepzIcon(icon = RepzIcon.Backspace, contentDescription = contentDescription)
    }
}

@Composable
internal fun ClearKey(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KeypadKeyButton(
        onClick = onClick,
        restContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        restContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun HideKey(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KeypadKeyButton(
        onClick = onClick,
        restContainerColor = Color.Transparent,
        restContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    ) {
        RepzIcon(icon = RepzIcon.KeyboardHide, contentDescription = contentDescription)
    }
}

/** Spans rows 3-4 (DESIGN_SYSTEM §9): `primary`/`onPrimary`, an icon over the destination label
 * (U1 "the Next key shows its destination label"), which wraps to 2 lines rather than truncating. */
@Composable
internal fun NextKey(
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KeypadKeyButton(
        onClick = onClick,
        restContainerColor = MaterialTheme.colorScheme.primary,
        restContentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = modifier.semantics(mergeDescendants = true) { this.contentDescription = contentDescription },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(RepzSpacing.xxs),
        ) {
            RepzIcon(icon = RepzIcon.ArrowForward, contentDescription = null)
            Text(label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}

/** One RPE choice (U2 option a): same key shape as a digit, selected value tinted
 * `secondaryContainer`/`onSecondaryContainer` and exposed as [selected] for accessibility. */
@Composable
internal fun RpeChoiceKey(
    value: FixedDecimal,
    symbols: RepzNumberSymbols,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor =
        if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest
    val contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
    KeypadKeyButton(
        onClick = onClick,
        restContainerColor = containerColor,
        restContentColor = contentColor,
        modifier = modifier.semantics { this.selected = selected },
    ) {
        Text(value.format(symbols), style = RepzTheme.typography.numericMedium)
    }
}

/**
 * The ± control (DESIGN_SYSTEM §9, MOT-10 U4b): same press morph/flash as [KeypadKeyButton] from
 * its own rest radius [PlusMinusRestCornerRadius] (`shapes.largeIncreased`), plus a long-press
 * repeat (250 ms, then 60 ms once held 1 s) that stops on release or on [onStep] returning `false`
 * (the step was refused). A plain tap (released before the first repeat fires) still gives exactly
 * one step from `clickable`'s own `onClick`; [didRepeatFire] suppresses that `onClick` once the
 * repeat loop has already taken over, so a long hold never double-counts its final step.
 * [contentDescription] replaces the merged announcement of [label] with the fuller "Increase/
 * decrease by …" text (U1); `clickable`'s own accessibility action reuses the same [onStep].
 */
@Composable
internal fun PlusMinusButton(
    label: String,
    contentDescription: String,
    enabled: Boolean,
    onStep: () -> Boolean,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val morphed = isPressed && enabled
    var didRepeatFire by remember { mutableStateOf(false) }
    val currentOnStep by rememberUpdatedState(onStep)

    LaunchedEffect(isPressed, enabled) {
        if (!isPressed || !enabled) {
            didRepeatFire = false
            return@LaunchedEffect
        }
        delay(InitialRepeatDelay)
        val start = TimeSource.Monotonic.markNow()
        while (isActive) {
            didRepeatFire = true
            if (!currentOnStep()) break
            val interval = if (start.elapsedNow() >= RepeatAccelerationThreshold) FastRepeatInterval else InitialRepeatDelay
            delay(interval)
        }
    }

    val cornerRadius by
        animateDpAsState(
            targetValue = if (morphed) KeyPressedCornerRadius else PlusMinusRestCornerRadius,
            animationSpec = RepzMotion.springs.pressMorph(),
        )
    val containerColor by
        animateColorAsState(
            targetValue = if (morphed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
            animationSpec = RepzMotion.effectsSprings.effectsFast(),
        )
    val contentColor by
        animateColorAsState(
            targetValue = if (morphed) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
            animationSpec = RepzMotion.effectsSprings.effectsFast(),
        )

    Box(
        modifier =
            modifier
                .size(PlusMinusSize)
                .clip(RoundedCornerShape(cornerRadius))
                .background(containerColor)
                .semantics { this.contentDescription = contentDescription }
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(),
                    enabled = enabled,
                    onClickLabel = contentDescription,
                    role = Role.Button,
                    onClick = { if (!didRepeatFire) onStep() },
                ).alpha(if (enabled) 1f else DISABLED_KEY_ALPHA),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Text(label, style = RepzTheme.typography.numericSmall, textAlign = TextAlign.Center)
        }
    }
}
