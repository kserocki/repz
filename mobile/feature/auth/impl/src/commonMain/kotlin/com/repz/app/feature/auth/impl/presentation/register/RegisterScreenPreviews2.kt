package com.repz.app.feature.auth.impl.presentation.register

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

private val SubmittingState =
    RegisterUiState(displayName = "Lena", email = "lena@example.com", password = "correct-horse-battery", isSubmitting = true)
private val OfflineState =
    RegisterUiState(
        displayName = "Lena",
        email = "lena@example.com",
        password = "correct-horse-battery",
        isOfflineBannerVisible = true,
    )

@Preview
@Composable
private fun RegisterSubmittingLightPreview() = RegisterPreview(darkTheme = false, state = SubmittingState)

@Preview
@Composable
private fun RegisterSubmittingDarkPreview() = RegisterPreview(darkTheme = true, state = SubmittingState)

@Preview
@Composable
private fun RegisterOfflineLightPreview() = RegisterPreview(darkTheme = false, state = OfflineState)

@Preview
@Composable
private fun RegisterOfflineDarkPreview() = RegisterPreview(darkTheme = true, state = OfflineState)
