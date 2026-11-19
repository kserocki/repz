package com.repz.app.core.designsystem.component.workout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.repz.app.core.designsystem.theme.RepzTheme
import kotlinx.collections.immutable.persistentListOf

private val TypeMenuItems =
    persistentListOf(
        RepzMenuItem(label = "Warm-up", onClick = {}),
        RepzMenuItem(label = "Normal", onClick = {}, isChecked = true),
        RepzMenuItem(label = "Drop set", onClick = {}),
        RepzMenuItem(label = "Failure", onClick = {}),
    )

@Composable
private fun RepzSetRowSample(
    modifier: Modifier = Modifier,
    badgeLabel: String = "4",
    type: RepzSetType = RepzSetType.Normal,
    previousText: String? = "82.5 × 8",
    weightText: String = "82.5",
    repsText: String = "8",
    isPrefilled: Boolean = false,
    editingField: RepzSetField? = null,
    isCompleted: Boolean = false,
    isPreviousColumnVisible: Boolean = true,
) {
    RepzSetRow(
        badgeLabel = badgeLabel,
        type = type,
        previousText = previousText,
        noPreviousText = "—",
        lastLineText = previousText?.let { "Last: $it" },
        weightText = weightText,
        repsText = repsText,
        isPrefilled = isPrefilled,
        editingField = editingField,
        isCompleted = isCompleted,
        isPreviousColumnVisible = isPreviousColumnVisible,
        typeMenuItems = TypeMenuItems,
        onWeightClick = {},
        onRepsClick = {},
        onCompleteClick = {},
        onDelete = {},
        accessibilityActions = persistentListOf(RepzAccessibilityAction(label = "Delete set", onAction = {})),
        completeContentDescription = "Complete set $badgeLabel, $weightText kilograms for $repsText reps",
        weightContentDescription = "Weight, set $badgeLabel",
        repsContentDescription = "Reps, set $badgeLabel",
        badgeContentDescription = "Set $badgeLabel type",
        modifier = modifier,
    )
}

@Composable
private fun RepzSetRowStatesSample() {
    Column {
        RepzSetRowSample(badgeLabel = "4", previousText = null, weightText = "", repsText = "")
        RepzSetRowSample(badgeLabel = "4", isPrefilled = true)
        RepzSetRowSample(badgeLabel = "4", editingField = RepzSetField.Weight)
        RepzSetRowSample(badgeLabel = "1", previousText = "80 × 8", weightText = "80", repsText = "8", isCompleted = true)
        RepzSetRowSample(badgeLabel = "W", type = RepzSetType.Warmup, previousText = "60 × 10", weightText = "60", repsText = "10")
        RepzSetRowSample(badgeLabel = "D", type = RepzSetType.Drop, previousText = null, weightText = "60", repsText = "12")
        RepzSetRowSample(badgeLabel = "F", type = RepzSetType.Failure, previousText = "70 × 9", weightText = "70", repsText = "10")
        RepzSetRowSample(badgeLabel = "2", previousText = "80 × 8", weightText = "82.5", repsText = "8", isPreviousColumnVisible = false)
    }
}

// A static mid-swipe look for a design review (board 05's own catalog draws one too): the real
// gesture lives entirely inside RepzSetRow's own state, so this reuses the same internal
// SetRowSwipeBackground behind a row shifted left through its public `modifier`.
@Composable
private fun RepzSetRowSwipingSample() {
    Box {
        SetRowSwipeBackground(modifier = Modifier.matchParentSize())
        RepzSetRowSample(badgeLabel = "4", modifier = Modifier.offset(x = (-96).dp))
    }
}

@Preview
@Composable
private fun RepzSetRowLightPreview() {
    RepzTheme(darkTheme = false) { RepzSetRowStatesSample() }
}

@Preview
@Composable
private fun RepzSetRowDarkPreview() {
    RepzTheme(darkTheme = true) { RepzSetRowStatesSample() }
}

@Preview
@Composable
private fun RepzSetRowSwipingLightPreview() {
    RepzTheme(darkTheme = false) { RepzSetRowSwipingSample() }
}

@Preview
@Composable
private fun RepzSetRowSwipingDarkPreview() {
    RepzTheme(darkTheme = true) { RepzSetRowSwipingSample() }
}

@Preview(fontScale = 2f)
@Composable
private fun RepzSetRowFontScale2Preview() {
    RepzTheme(darkTheme = false) { RepzSetRowSample(badgeLabel = "3", previousText = "80 × 7", weightText = "82.5", repsText = "8") }
}
