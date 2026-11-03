package com.repz.app.core.domain.session

/** Mirrors `core:network`'s `SessionStatus` on purpose (ADR-0025): features see only this type. */
sealed interface SessionState {
    /** The stored session hasn't been read yet. */
    data object Unknown : SessionState

    data object SignedIn : SessionState

    data class SignedOut(
        val reason: SignOutReason,
    ) : SessionState
}

enum class SignOutReason {
    NoSession,
    LoggedOut,
    Expired,
}
