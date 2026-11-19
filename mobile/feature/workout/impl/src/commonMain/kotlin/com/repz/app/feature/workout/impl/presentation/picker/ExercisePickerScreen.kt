package com.repz.app.feature.workout.impl.presentation.picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.repz.app.core.designsystem.theme.RepzTheme
import com.repz.app.core.navigation.Navigator
import org.koin.compose.viewmodel.koinViewModel

/** Stateful (ADR-0005): resolves [ExercisePickerViewModel] itself and forwards
 * [ExercisePickerEffect.Close] (exercise picked, or the back button) to [navigator]. The rest of
 * the per-state previews live in `ExercisePickerScreenPreviews.kt`. */
@Composable
fun ExercisePickerScreen(
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<ExercisePickerViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ExercisePickerEffect.Close -> navigator.goBack()
            }
        }
    }

    ExercisePickerContent(
        state = state,
        onIntent = viewModel::onIntent,
        onBackClick = { viewModel.onIntent(ExercisePickerIntent.BackClicked) },
        modifier = modifier,
    )
}

@Preview
@Composable
private fun ExercisePickerScreenDefaultPreview() {
    RepzTheme(darkTheme = false) {
        ExercisePickerContent(state = ExercisePickerUiState(isLoading = false), onIntent = {}, onBackClick = {})
    }
}
