package com.repz.app.core.designsystem.component.workout

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.theme.RepzTheme
import kotlinx.collections.immutable.persistentListOf

private val OverflowItems = persistentListOf(RepzMenuItem(label = "Discard workout", onClick = {}, isDestructive = true))

@Composable
private fun RepzLiveWorkoutHeaderSample(isFinishEnabled: Boolean = true) {
    RepzLiveWorkoutHeader(
        name = "Push A",
        elapsedText = "34:12",
        isFinishEnabled = isFinishEnabled,
        finishLabel = "Finish",
        isEditingName = false,
        onNameClick = {},
        onNameChange = {},
        onNameEditingFinish = {},
        onMinimizeClick = {},
        onFinishClick = {},
        overflowItems = OverflowItems,
        minimizeContentDescription = "Minimize workout",
        editNameContentDescription = "Edit workout name",
        moreOptionsContentDescription = "Workout options",
    )
}

@Composable
private fun RepzLiveWorkoutHeaderEditingSample() {
    RepzLiveWorkoutHeader(
        name = "Push A",
        elapsedText = "34:12",
        isFinishEnabled = true,
        finishLabel = "Finish",
        isEditingName = true,
        onNameClick = {},
        onNameChange = {},
        onNameEditingFinish = {},
        onMinimizeClick = {},
        onFinishClick = {},
        overflowItems = OverflowItems,
        minimizeContentDescription = "Minimize workout",
        editNameContentDescription = "Edit workout name",
        moreOptionsContentDescription = "Workout options",
    )
}

@Preview
@Composable
private fun RepzLiveWorkoutHeaderLightPreview() {
    RepzTheme(darkTheme = false) { RepzLiveWorkoutHeaderSample() }
}

@Preview
@Composable
private fun RepzLiveWorkoutHeaderDarkPreview() {
    RepzTheme(darkTheme = true) { RepzLiveWorkoutHeaderSample() }
}

@Preview
@Composable
private fun RepzLiveWorkoutHeaderEditingLightPreview() {
    RepzTheme(darkTheme = false) { RepzLiveWorkoutHeaderEditingSample() }
}

@Preview
@Composable
private fun RepzLiveWorkoutHeaderEditingDarkPreview() {
    RepzTheme(darkTheme = true) { RepzLiveWorkoutHeaderEditingSample() }
}

@Preview
@Composable
private fun RepzLiveWorkoutHeaderFinishDisabledLightPreview() {
    RepzTheme(darkTheme = false) { RepzLiveWorkoutHeaderSample(isFinishEnabled = false) }
}

@Preview
@Composable
private fun RepzLiveWorkoutHeaderFinishDisabledDarkPreview() {
    RepzTheme(darkTheme = true) { RepzLiveWorkoutHeaderSample(isFinishEnabled = false) }
}

@Preview(fontScale = 2f)
@Composable
private fun RepzLiveWorkoutHeaderFontScale2Preview() {
    RepzTheme(darkTheme = false) { RepzLiveWorkoutHeaderSample() }
}
