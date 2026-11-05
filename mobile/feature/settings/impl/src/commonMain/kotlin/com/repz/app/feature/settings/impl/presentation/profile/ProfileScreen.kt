package com.repz.app.feature.settings.impl.presentation.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.repz.app.core.designsystem.theme.RepzTheme
import org.koin.compose.viewmodel.koinViewModel

/**
 * Stateful (ADR-0005): resolves [ProfileViewModel] itself. No `navigator` parameter: Profile has
 * nothing to navigate to until a later issue adds sub-screens of its own. [ProfileEffect] has no
 * subtypes yet, since navigation after logout is the auth gate's job, not an effect; there is
 * nothing to collect until a subtype is added. The rest of the per-state previews live in
 * `ProfileScreenPreviews.kt`.
 */
@Composable
fun ProfileScreen(modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<ProfileViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

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
