package com.repz.app.feature.settings.impl.presentation.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.theme.RepzTheme

/** Shared with `ProfileScreen.kt` (same package, `internal` so no import is needed);
 * `ProfileScreen.kt` keeps its own single preview for Konsist N4. */
internal val ProfileSignedInState =
    ProfileUiState(account = ProfileAccount(displayName = "Alex Carter", email = "alex@repz.app", monogram = "A"))

internal val ProfilePlaceholderState = ProfileUiState(account = null)

internal val ProfileDialogOpenState = ProfileSignedInState.copy(isLogOutDialogVisible = true)

internal val ProfileLongNameState =
    ProfileUiState(
        account =
            ProfileAccount(
                displayName = "Małgorzata Wiśniewska-Kowalczyk",
                email = "malgorzata.wisniewska-kowalczyk@example.com",
                monogram = "M",
            ),
    )

@Composable
private fun ProfilePreview(
    darkTheme: Boolean,
    state: ProfileUiState,
) {
    RepzTheme(darkTheme = darkTheme) {
        ProfileContent(state = state, onIntent = {})
    }
}

@Preview
@Composable
private fun ProfileSignedInDarkPreview() = ProfilePreview(darkTheme = true, state = ProfileSignedInState)

@Preview
@Composable
private fun ProfilePlaceholderLightPreview() = ProfilePreview(darkTheme = false, state = ProfilePlaceholderState)

@Preview
@Composable
private fun ProfilePlaceholderDarkPreview() = ProfilePreview(darkTheme = true, state = ProfilePlaceholderState)

@Preview
@Composable
private fun ProfileDialogOpenLightPreview() = ProfilePreview(darkTheme = false, state = ProfileDialogOpenState)

@Preview
@Composable
private fun ProfileDialogOpenDarkPreview() = ProfilePreview(darkTheme = true, state = ProfileDialogOpenState)

@Preview
@Composable
private fun ProfileLongNameLightPreview() = ProfilePreview(darkTheme = false, state = ProfileLongNameState)

@Preview(locale = "pl")
@Composable
private fun ProfileLongNamePolishPreview() = ProfilePreview(darkTheme = false, state = ProfileLongNameState)

@Preview(locale = "pl")
@Composable
private fun ProfileDialogOpenPolishPreview() = ProfilePreview(darkTheme = false, state = ProfileDialogOpenState)
