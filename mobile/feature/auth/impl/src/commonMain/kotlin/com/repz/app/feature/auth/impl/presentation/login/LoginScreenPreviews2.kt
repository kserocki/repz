package com.repz.app.feature.auth.impl.presentation.login

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

private val SubmittingState = LoginUiState(email = "lena@example.com", password = "correct-horse-battery", isSubmitting = true)
private val OfflineState = LoginUiState(email = "lena@example.com", password = "correct-horse-battery", isOfflineBannerVisible = true)

@Preview
@Composable
private fun LoginSubmittingLightPreview() = LoginPreview(darkTheme = false, state = SubmittingState)

@Preview
@Composable
private fun LoginSubmittingDarkPreview() = LoginPreview(darkTheme = true, state = SubmittingState)

@Preview
@Composable
private fun LoginOfflineLightPreview() = LoginPreview(darkTheme = false, state = OfflineState)

@Preview
@Composable
private fun LoginOfflineDarkPreview() = LoginPreview(darkTheme = true, state = OfflineState)
