package com.repz.app.feature.settings.impl.presentation.profile

/**
 * [account] is `null` until the Room row is known: never-cached offline shows the Q9 placeholder
 * header, never an error. [isLoggingOut] never goes back to `false` once set (the auth gate
 * replaces this screen, the same reasoning as the login screen's `isSubmitting`).
 */
data class ProfileUiState(
    val account: ProfileAccount? = null,
    val isLogOutDialogVisible: Boolean = false,
    val isLoggingOut: Boolean = false,
)

data class ProfileAccount(
    val displayName: String,
    val email: String,
    val monogram: String,
)
