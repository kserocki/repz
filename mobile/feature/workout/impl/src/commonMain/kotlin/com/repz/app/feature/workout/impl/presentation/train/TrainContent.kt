package com.repz.app.feature.workout.impl.presentation.train

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import com.repz.app.core.designsystem.component.RepzButton
import com.repz.app.core.designsystem.component.RepzButtonSize
import com.repz.app.core.designsystem.component.RepzButtonStyle
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.feature.workout.impl.generated.resources.Res
import com.repz.app.feature.workout.impl.generated.resources.train_resume
import com.repz.app.feature.workout.impl.generated.resources.train_start_workout
import com.repz.app.feature.workout.impl.generated.resources.train_template_exercise_count
import com.repz.app.feature.workout.impl.generated.resources.train_template_start
import com.repz.app.feature.workout.impl.generated.resources.train_templates_title
import com.repz.app.feature.workout.impl.generated.resources.train_workout_in_progress
import com.repz.app.feature.workout.impl.generated.resources.workout_default_name_friday
import com.repz.app.feature.workout.impl.generated.resources.workout_default_name_monday
import com.repz.app.feature.workout.impl.generated.resources.workout_default_name_saturday
import com.repz.app.feature.workout.impl.generated.resources.workout_default_name_sunday
import com.repz.app.feature.workout.impl.generated.resources.workout_default_name_thursday
import com.repz.app.feature.workout.impl.generated.resources.workout_default_name_tuesday
import com.repz.app.feature.workout.impl.generated.resources.workout_default_name_wednesday
import kotlinx.datetime.DayOfWeek
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The Train tab's v0 root (TODO(REP-32): the real home screen with the hero split button, weekly
 * ring and FAB menu). An empty screen while [TrainUiState.isLoading] (the first Room emission
 * hasn't arrived yet) avoids flashing "Start workout" before [TrainUiState.activeWorkoutName] is
 * known. [TrainUiState.templates] section is omitted entirely when there are no templates yet
 * (template creation isn't built in the app yet; QA seeds one through the API).
 */
@Composable
fun TrainContent(
    state: TrainUiState,
    onIntent: (TrainIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) {
        Box(modifier = modifier.fillMaxSize())
        return
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(RepzSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(RepzSpacing.lg),
    ) {
        val activeWorkoutName = state.activeWorkoutName
        if (activeWorkoutName != null) {
            ActiveWorkoutResumeCard(
                name = activeWorkoutName,
                onResumeClick = { onIntent(TrainIntent.ResumeClicked) },
            )
        } else {
            val defaultName = stringResource(state.today.defaultWorkoutNameResource())
            RepzButton(
                text = stringResource(Res.string.train_start_workout),
                onClick = { onIntent(TrainIntent.StartEmptyWorkoutClicked(defaultName)) },
                modifier = Modifier.fillMaxWidth().testTag("train_start_empty"),
                style = RepzButtonStyle.Filled,
                size = RepzButtonSize.Medium,
            )
        }

        if (state.templates.isNotEmpty()) {
            TemplatesSection(
                templates = state.templates,
                onStartClick = { templateId -> onIntent(TrainIntent.StartTemplateClicked(templateId)) },
            )
        }
    }
}

@Composable
private fun ActiveWorkoutResumeCard(
    name: String,
    onResumeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(RepzSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(RepzSpacing.sm),
    ) {
        Text(
            text = stringResource(Res.string.train_workout_in_progress),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        RepzButton(
            text = stringResource(Res.string.train_resume),
            onClick = onResumeClick,
            modifier = Modifier.fillMaxWidth().testTag("train_resume"),
            style = RepzButtonStyle.Filled,
            size = RepzButtonSize.Medium,
        )
    }
}

@Composable
private fun TemplatesSection(
    templates: List<TemplateItemUi>,
    onStartClick: (com.repz.app.core.domain.model.WorkoutTemplateId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
        Text(stringResource(Res.string.train_templates_title), style = MaterialTheme.typography.titleMedium)
        templates.forEachIndexed { index, template ->
            TemplateRow(
                template = template,
                index = index,
                onStartClick = { onStartClick(template.id) },
            )
        }
    }
}

@Composable
private fun TemplateRow(
    template: TemplateItemUi,
    index: Int,
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(RepzSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RepzSpacing.md),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = template.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = pluralStringResource(Res.plurals.train_template_exercise_count, template.exerciseCount, template.exerciseCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        RepzButton(
            text = stringResource(Res.string.train_template_start),
            onClick = onStartClick,
            modifier = Modifier.testTag("train_template_${index}_start"),
            style = RepzButtonStyle.Outlined,
            size = RepzButtonSize.Small,
        )
    }
}

private fun DayOfWeek.defaultWorkoutNameResource(): StringResource =
    when (this) {
        DayOfWeek.MONDAY -> Res.string.workout_default_name_monday
        DayOfWeek.TUESDAY -> Res.string.workout_default_name_tuesday
        DayOfWeek.WEDNESDAY -> Res.string.workout_default_name_wednesday
        DayOfWeek.THURSDAY -> Res.string.workout_default_name_thursday
        DayOfWeek.FRIDAY -> Res.string.workout_default_name_friday
        DayOfWeek.SATURDAY -> Res.string.workout_default_name_saturday
        DayOfWeek.SUNDAY -> Res.string.workout_default_name_sunday
    }
