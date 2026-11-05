package com.repz.app.feature.settings.impl.presentation.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.repz.app.core.designsystem.theme.RepzTheme
import org.koin.compose.viewmodel.koinViewModel

/**
 * Stateful (ADR-0005): resolves [ProfileViewModel] itself. No `navigator` parameter yet (Q5: no
 * sub-screens before Profile grows settings of its own). [ProfileEffect] has no subtypes (Q10:
 * navigation after logout is the auth gate's job, not an effect) — the collection below never runs,
 * but keeps the same shape every other `*Screen` has. The rest of the per-state previews live in
 * `ProfileScreenPreviews.kt`.
 */
@Composable
fun ProfileScreen(modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<ProfileViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        // ProfileEffect has no subtypes (Q10): nothing is ever emitted, so there's nothing to branch on.
        viewModel.effects.collect {}
    }

    ProfileContent(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun ProfileScreenDefaultPreview() {
    RepzTheme(darkTheme = false) {
        ProfileContent(state = ProfileSignedInState, onIntent = {})
    }
}
