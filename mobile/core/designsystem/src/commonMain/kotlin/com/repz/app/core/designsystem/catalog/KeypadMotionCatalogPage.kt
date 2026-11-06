package com.repz.app.core.designsystem.catalog

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.repz.app.core.designsystem.format.FixedDecimal
import com.repz.app.core.designsystem.format.RepzNumberSymbols
import com.repz.app.core.designsystem.format.rememberRepzNumberSymbols
import com.repz.app.core.designsystem.haptics.LocalRepzHaptics
import com.repz.app.core.designsystem.haptics.RepzHaptics
import com.repz.app.core.designsystem.haptics.rememberPlatformHapticPerformer
import com.repz.app.core.designsystem.keypad.DigitKey
import com.repz.app.core.designsystem.keypad.KeypadField
import com.repz.app.core.designsystem.keypad.KeypadMode
import com.repz.app.core.designsystem.keypad.KeypadNextAction
import com.repz.app.core.designsystem.keypad.NumberKeypadRequest
import com.repz.app.core.designsystem.keypad.NumberKeypadSheet
import com.repz.app.core.designsystem.keypad.PlusMinusButton
import com.repz.app.core.designsystem.theme.RepzSpacing
import kotlinx.coroutines.delay

private const val MOTION_LOOP_INTERVAL_MILLIS = 4000L
private const val SETTLE_DELAY_MILLIS = 400L
private const val DIGIT_PRESS_HOLD_MILLIS = 150L
private const val GAP_BETWEEN_DIGITS_MILLIS = 250L
private const val BEFORE_LONG_PRESS_DELAY_MILLIS = 300L
private const val LONG_PRESS_HOLD_MILLIS = 2000L
private const val MOTION_WEIGHT_MAX_HUNDREDTHS = 1000_00L
private const val MOTION_WEIGHT_STEP_HUNDREDTHS = 250L
private const val MOTION_SAMPLE_WEIGHT_HUNDREDTHS = 8250L
private val MOTION_SAMPLE_WEIGHT = FixedDecimal(MOTION_SAMPLE_WEIGHT_HUNDREDTHS)
private val MOTION_WEIGHT_MAX = FixedDecimal(MOTION_WEIGHT_MAX_HUNDREDTHS)
private const val MOTION_PRESS_DEMO_LABEL = "press demo (same key composables as the grid)"
private val MOTION_WEIGHT_STEP = FixedDecimal(MOTION_WEIGHT_STEP_HUNDREDTHS)
private val DEMO_DIGITS = listOf(1, 0, 2)

/**
 * `keypad-motion`: auto-plays every [MOTION_LOOP_INTERVAL_MILLIS] since `simctl`/instrumentation
 * can't synthesize real touches (F2's established catalog pattern, [rememberAutoPlayState]). The
 * real [NumberKeypadSheet] slides in and out for real (its own `AnimatedVisibility`/spring); the
 * key-press morph+flash and the ± long-press repeat are demonstrated on standalone instances of the
 * exact same [DigitKey]/[PlusMinusButton] composables the sheet's own grid uses, each driven by a
 * real [PressInteraction] on an externally held [MutableInteractionSource] (plan §3.8's "presses …
 * via PressInteraction on the internal keys") — the sheet's own grid keys aren't reachable from
 * outside its public `request`/callback surface, so this proves the identical key composable
 * instead of reaching into the live grid.
 */
private fun motionRequest(
    visible: Boolean,
    value: FixedDecimal?,
): NumberKeypadRequest? {
    if (!visible) return null
    return NumberKeypadRequest(
        targetKey = "catalog-motion",
        field =
            KeypadField(
                mode = KeypadMode.Weight,
                value = value,
                min = FixedDecimal.ZERO,
                max = MOTION_WEIGHT_MAX,
                step = MOTION_WEIGHT_STEP,
                fractionDigits = 2,
                sameAsLast = MOTION_SAMPLE_WEIGHT,
                quickValues = emptyList(),
                choices = emptyList(),
            ),
        modes = listOf(KeypadMode.Weight, KeypadMode.Reps),
        weightUnitLabel = "kg",
        contextLabel = CatalogSpecimens.KEYPAD_CONTEXT_LABEL,
        nextAction = KeypadNextAction.Reps,
        stepOptions = emptyList(),
    )
}

/** Presses 1-0-2, then holds + for [LONG_PRESS_HOLD_MILLIS], each as a real [PressInteraction] on
 * its own held [MutableInteractionSource] (see [KeypadMotionCatalogPage]'s doc). */
private suspend fun playMotionSequence(
    digitInteractionSources: List<MutableInteractionSource>,
    plusInteractionSource: MutableInteractionSource,
) {
    delay(SETTLE_DELAY_MILLIS)
    for (source in digitInteractionSources) {
        val press = PressInteraction.Press(Offset.Zero)
        source.tryEmit(press)
        delay(DIGIT_PRESS_HOLD_MILLIS)
        source.tryEmit(PressInteraction.Release(press))
        delay(GAP_BETWEEN_DIGITS_MILLIS)
    }
    delay(BEFORE_LONG_PRESS_DELAY_MILLIS)
    val press = PressInteraction.Press(Offset.Zero)
    plusInteractionSource.tryEmit(press)
    delay(LONG_PRESS_HOLD_MILLIS)
    plusInteractionSource.tryEmit(PressInteraction.Release(press))
}

@Composable
private fun MotionPressDemoRow(
    symbols: RepzNumberSymbols,
    digitInteractionSources: List<MutableInteractionSource>,
    plusInteractionSource: MutableInteractionSource,
) {
    CatalogSectionLabel(MOTION_PRESS_DEMO_LABEL)
    Row(horizontalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
        for ((index, digit) in DEMO_DIGITS.withIndex()) {
            DigitKey(digit = digit, onClick = {}, interactionSource = digitInteractionSources[index])
        }
        val stepText = MOTION_WEIGHT_STEP.format(symbols)
        val increaseLabel = "+$stepText"
        val increaseDescription = "Increase by $stepText kg"
        PlusMinusButton(
            label = increaseLabel,
            contentDescription = increaseDescription,
            enabled = true,
            onStep = { true },
            interactionSource = plusInteractionSource,
        )
    }
}

@Composable
internal fun KeypadMotionCatalogPage() {
    val visible = rememberAutoPlayState(intervalMillis = MOTION_LOOP_INTERVAL_MILLIS)
    val symbols = rememberRepzNumberSymbols()
    var value by remember { mutableStateOf<FixedDecimal?>(MOTION_SAMPLE_WEIGHT) }
    val digitInteractionSources = remember { DEMO_DIGITS.map { MutableInteractionSource() } }
    val plusInteractionSource = remember { MutableInteractionSource() }
    val platformPerformer = rememberPlatformHapticPerformer()
    val hapticLog = remember(platformPerformer) { CatalogHapticLog(platformPerformer) }
    val eventLog = rememberCatalogEventLog()

    LaunchedEffect(visible.value) {
        if (visible.value) playMotionSequence(digitInteractionSources, plusInteractionSource)
    }

    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.md)) {
        ReducedMotionHeader()
        CompositionLocalProvider(LocalRepzHaptics provides RepzHaptics(hapticLog, enabled = true)) {
            NumberKeypadSheet(
                request = motionRequest(visible.value, value),
                onValueChange = { newValue ->
                    value = newValue
                    eventLog.lastValue = "value: ${newValue?.format(symbols) ?: "(empty)"}"
                },
                onModeSelect = { mode -> eventLog.lastMode = "mode: $mode" },
                onStepSelect = {},
                onNext = { eventLog.lastAction = "next" },
                onDismiss = { eventLog.lastAction = "dismiss" },
            )
        }
        MotionPressDemoRow(symbols, digitInteractionSources, plusInteractionSource)
        CatalogEventLogColumn(eventLog)
        CatalogHapticLogColumn(hapticLog.entries)
    }
}
