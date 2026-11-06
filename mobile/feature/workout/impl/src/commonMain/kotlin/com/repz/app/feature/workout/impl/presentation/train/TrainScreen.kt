package com.repz.app.feature.workout.impl.presentation.train

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.repz.app.core.designsystem.theme.RepzTheme
import com.repz.app.core.navigation.Navigator
import com.repz.app.feature.workout.api.LiveWorkoutRoute
import org.koin.compose.viewmodel.koinViewModel

/** Stateful (ADR-0005): resolves [TrainViewModel] itself and forwards [TrainEffect.OpenLiveWorkout]
 * to [navigator]. The rest of the per-state previews live in `TrainScreenPreviews.kt`. */
@Composable
fun TrainScreen(
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<TrainViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                TrainEffect.OpenLiveWorkout -> navigator.navigate(LiveWorkoutRoute)
            }
        }
    }

    TrainContent(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

@Preview
@Composable
private fun TrainScreenDefaultPreview() {
    RepzTheme(darkTheme = false) {
        TrainContent(state = TrainUiState(isLoading = false), onIntent = {})
    }
}
