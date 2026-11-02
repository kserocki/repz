package com.repz.app.core.designsystem.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.repz.app.core.designsystem.haptics.RepzHapticEvent
import com.repz.app.core.designsystem.haptics.RepzHaptics
import com.repz.app.core.designsystem.haptics.rememberPlatformHapticPerformer
import com.repz.app.core.designsystem.theme.RepzSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val HAPTICS_AUTO_PLAY_STEP_MILLIS = 700L

private class HapticDemo(
    val label: String,
    val event: RepzHapticEvent,
    val action: RepzHaptics.() -> Unit,
)

private val HAPTIC_DEMOS =
    listOf(
        HapticDemo("Tick", RepzHapticEvent.Tick) { tick() },
        HapticDemo("Confirm", RepzHapticEvent.Confirm) { confirm() },
        HapticDemo("RestEnd", RepzHapticEvent.RestEnd) { restEnd() },
        HapticDemo("Success", RepzHapticEvent.Success) { success() },
        HapticDemo("Reject", RepzHapticEvent.Reject) { reject() },
        HapticDemo("DragStart", RepzHapticEvent.DragStart) { dragStart() },
        HapticDemo("DragEnd", RepzHapticEvent.DragEnd) { dragEnd() },
    )

/**
 * `haptics` (MOTION.md §3): the 7 events as buttons, a "Haptics enabled" switch that re-provides
 * `RepzHaptics` with `enabled = false`, a "Play all" button and an on-screen event log — the only
 * way to prove an event fired without a physical device.
 */
@Composable
internal fun HapticsCatalogPage() {
    val platformPerformer = rememberPlatformHapticPerformer()
    val log = remember(platformPerformer) { CatalogHapticLog(platformPerformer) }
    var hapticsEnabled by remember { mutableStateOf(true) }
    val haptics = remember(log, hapticsEnabled) { RepzHaptics(log, hapticsEnabled) }
    val scope = rememberCoroutineScope()

    suspend fun playAll() {
        HAPTIC_DEMOS.forEach { demo ->
            if (!hapticsEnabled) log.recordBlocked(demo.event)
            demo.action(haptics)
            delay(HAPTICS_AUTO_PLAY_STEP_MILLIS)
        }
    }

    LaunchedEffect(Unit) { playAll() }

    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
            Text(
                text = CatalogSpecimens.HAPTICS_ENABLED_LABEL,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Switch(checked = hapticsEnabled, onCheckedChange = { hapticsEnabled = it })
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(RepzSpacing.sm), verticalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
            HAPTIC_DEMOS.forEach { demo ->
                Button(
                    onClick = {
                        if (!hapticsEnabled) log.recordBlocked(demo.event)
                        demo.action(haptics)
                    },
                ) {
                    Text(demo.label)
                }
            }
        }

        Button(onClick = { scope.launch { playAll() } }) { Text(CatalogSpecimens.PLAY_ALL_LABEL) }

        CatalogSectionLabel("event log")
        Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.xxs)) {
            log.entries.forEach { entry ->
                Text(text = entry, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
