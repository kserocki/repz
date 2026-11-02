package com.repz.app.core.network.session

sealed interface SessionStatus {
    /** Before [SessionManager.restore] has run once. */
    data object Unknown : SessionStatus

    data object SignedIn : SessionStatus

    data class SignedOut(
        val reason: SignOutReason,
    ) : SessionStatus
}

enum class SignOutReason {
    NoSession,
    LoggedOut,
    Expired,
}
