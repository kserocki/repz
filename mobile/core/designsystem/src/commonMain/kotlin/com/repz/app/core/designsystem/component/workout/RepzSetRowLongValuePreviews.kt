package com.repz.app.core.designsystem.component.workout

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.theme.RepzTheme
import kotlinx.collections.immutable.persistentListOf

// The domain's declared weight maximum (ValidationLimits.SET_WEIGHT_MAX = 1000 kg) converted to
// lb: the longest string formatSetWeight can ever produce, and the auto-size fix's worst case.
private const val LONG_LB_VALUE = "2204.62"

@Composable
private fun RepzSetRowLongValueSample(isPreviousColumnVisible: Boolean) {
    RepzSetRow(
        badgeLabel = "3",
        type = RepzSetType.Normal,
        previousText = "$LONG_LB_VALUE × 8",
        noPreviousText = "—",
        lastLineText = "Last: $LONG_LB_VALUE × 8",
        weightText = LONG_LB_VALUE,
        repsText = "8",
        isPrefilled = false,
        editingField = null,
        isCompleted = false,
        isPreviousColumnVisible = isPreviousColumnVisible,
        typeMenuItems = persistentListOf(),
        onWeightClick = {},
        onRepsClick = {},
        onCompleteClick = {},
        onDelete = {},
        accessibilityActions = persistentListOf(),
        completeContentDescription = "Complete set 3, $LONG_LB_VALUE pounds for 8 reps",
        weightContentDescription = "Weight, set 3",
        repsContentDescription = "Reps, set 3",
        badgeContentDescription = "Set 3 type",
    )
}

@Preview(widthDp = 400)
@Composable
private fun RepzSetRowLongValueNormalLightPreview() {
    RepzTheme(darkTheme = false) { RepzSetRowLongValueSample(isPreviousColumnVisible = true) }
}

@Preview(widthDp = 400)
@Composable
private fun RepzSetRowLongValueNormalDarkPreview() {
    RepzTheme(darkTheme = true) { RepzSetRowLongValueSample(isPreviousColumnVisible = true) }
}

@Preview(widthDp = 320)
@Composable
private fun RepzSetRowLongValueNarrowLightPreview() {
    RepzTheme(darkTheme = false) { RepzSetRowLongValueSample(isPreviousColumnVisible = false) }
}

@Preview(widthDp = 320)
@Composable
private fun RepzSetRowLongValueNarrowDarkPreview() {
    RepzTheme(darkTheme = true) { RepzSetRowLongValueSample(isPreviousColumnVisible = false) }
}
