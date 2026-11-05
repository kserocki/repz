package com.repz.app.feature.auth.impl.presentation.welcome

/** Skip is pure UI (it scrolls the pager), so it has no intent here. */
sealed interface WelcomeIntent {
    data object CreateAccountClicked : WelcomeIntent

    data object LogInClicked : WelcomeIntent
}
