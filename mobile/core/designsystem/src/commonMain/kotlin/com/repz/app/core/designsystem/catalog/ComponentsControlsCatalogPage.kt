package com.repz.app.core.designsystem.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.repz.app.core.designsystem.component.RepzSlider
import com.repz.app.core.designsystem.component.RepzSwitch
import com.repz.app.core.designsystem.component.RepzTextField
import com.repz.app.core.designsystem.component.RepzTextFieldStyle
import com.repz.app.core.designsystem.haptics.LocalRepzHaptics
import com.repz.app.core.designsystem.haptics.RepzHaptics
import com.repz.app.core.designsystem.haptics.rememberPlatformHapticPerformer
import com.repz.app.core.designsystem.theme.RepzSpacing

// Board 04 §21 "Default rest": 0 to 5:00 (300 s) in 15 s steps.
private const val REST_SLIDER_MAX_SECONDS = 300f
private const val REST_SLIDER_STEPS = 19
private const val SECONDS_PER_MINUTE = 60
private const val MIN_TWO_DIGIT_SECONDS = 10

private fun formatMinutesSeconds(totalSeconds: Float): String {
    val whole = totalSeconds.toInt()
    val minutes = whole / SECONDS_PER_MINUTE
    val seconds = whole % SECONDS_PER_MINUTE
    val secondsText = if (seconds < MIN_TWO_DIGIT_SECONDS) "0$seconds" else "$seconds"
    return "$minutes:$secondsText"
}

/** `components-controls` (DESIGN_SYSTEM §9 filled `RepzTextField`/`RepzSwitch`/`RepzSlider`): the
 * filled field (unfocused, so the soft keyboard doesn't hide the slider/haptic log below it) and in
 * error, the switch on/off/disabled, the slider from 0 to 5:00 in 15 s steps, and the catalog haptic
 * log proving each step crossed fires Tick (MOTION.md §3). */
@Composable
internal fun ComponentsControlsCatalogPage() {
    val platformPerformer = rememberPlatformHapticPerformer()
    val log = remember(platformPerformer) { CatalogHapticLog(platformPerformer) }
    val loggedHaptics = remember(log) { RepzHaptics(log, enabled = true) }

    var dynamicColorEnabled by remember { mutableStateOf(true) }
    var hapticsEnabled by remember { mutableStateOf(false) }
    var restSeconds by remember { mutableFloatStateOf(150f) }

    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.lg)) {
        Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.md)) {
            CatalogSectionLabel("filled field")
            RepzTextField(
                value = "alex@repz.app",
                onValueChange = {},
                label = "Email",
                style = RepzTextFieldStyle.Filled,
                modifier = Modifier.fillMaxWidth(),
            )
            RepzTextField(
                value = "not-an-email",
                onValueChange = {},
                label = "Email",
                isError = true,
                style = RepzTextFieldStyle.Filled,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.md)) {
            CatalogSectionLabel("switch")
            SwitchRow(label = "Dynamic color", checked = dynamicColorEnabled, onCheckedChange = { dynamicColorEnabled = it })
            SwitchRow(label = "Haptics", checked = hapticsEnabled, onCheckedChange = { hapticsEnabled = it })
            SwitchRow(label = "Reduced motion (disabled)", checked = true, onCheckedChange = null, enabled = false)
        }

        CompositionLocalProvider(LocalRepzHaptics provides loggedHaptics) {
            Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    CatalogSectionLabel("slider")
                    Text(formatMinutesSeconds(restSeconds), style = MaterialTheme.typography.labelLarge)
                }
                RepzSlider(
                    value = restSeconds,
                    onValueChange = { restSeconds = it },
                    valueText = formatMinutesSeconds(restSeconds),
                    modifier = Modifier.fillMaxWidth(),
                    valueRange = 0f..REST_SLIDER_MAX_SECONDS,
                    steps = REST_SLIDER_STEPS,
                )
            }
        }

        CatalogSectionLabel("event log")
        Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.xxs)) {
            log.entries.forEach { entry ->
                Text(text = entry, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(RepzSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        RepzSwitch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}
