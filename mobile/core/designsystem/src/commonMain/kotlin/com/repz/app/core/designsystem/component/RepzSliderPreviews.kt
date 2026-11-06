package com.repz.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.designsystem.theme.RepzTheme

// Board 04 §21 "Default rest": 0 to 5:00 (300 s) in 15 s steps, 19 intermediate steps.
private const val DEFAULT_REST_MAX_SECONDS = 300f
private const val DEFAULT_REST_STEPS = 19

@Composable
private fun RepzSliderShowcase() {
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.lg)) {
        RepzSlider(
            value = 0.3f,
            onValueChange = {},
            valueText = "30%",
            modifier = Modifier.fillMaxWidth(),
        )
        RepzSlider(
            value = 150f,
            onValueChange = {},
            valueText = "2:30",
            modifier = Modifier.fillMaxWidth(),
            valueRange = 0f..DEFAULT_REST_MAX_SECONDS,
            steps = DEFAULT_REST_STEPS,
        )
        RepzSlider(
            value = 0f,
            onValueChange = {},
            valueText = "0:00",
            modifier = Modifier.fillMaxWidth(),
            valueRange = 0f..DEFAULT_REST_MAX_SECONDS,
            steps = DEFAULT_REST_STEPS,
        )
        RepzSlider(
            value = DEFAULT_REST_MAX_SECONDS,
            onValueChange = {},
            valueText = "5:00",
            modifier = Modifier.fillMaxWidth(),
            valueRange = 0f..DEFAULT_REST_MAX_SECONDS,
            steps = DEFAULT_REST_STEPS,
        )
        RepzSlider(
            value = 150f,
            onValueChange = {},
            valueText = "2:30",
            modifier = Modifier.fillMaxWidth(),
            valueRange = 0f..DEFAULT_REST_MAX_SECONDS,
            steps = DEFAULT_REST_STEPS,
            enabled = false,
        )
    }
}

@Preview
@Composable
private fun RepzSliderLightPreview() {
    RepzTheme(darkTheme = false) { RepzSliderShowcase() }
}

@Preview
@Composable
private fun RepzSliderDarkPreview() {
    RepzTheme(darkTheme = true) { RepzSliderShowcase() }
}
