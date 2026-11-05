package com.repz.app.feature.settings.impl.presentation.profile

sealed interface ProfileIntent {
    data object LogOutClicked : ProfileIntent

    data object LogOutConfirmed : ProfileIntent

    /** Cancel, system Back, and a scrim tap all send this. */
    data object LogOutDismissed : ProfileIntent
}
