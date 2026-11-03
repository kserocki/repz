package com.repz.app.core.data.session

import com.repz.app.core.domain.session.SessionState
import com.repz.app.core.domain.session.SignOutReason
import com.repz.app.core.network.session.SessionStatus
import com.repz.app.core.network.session.SignOutReason as NetworkSignOutReason

fun SessionStatus.toSessionState(): SessionState =
    when (this) {
        is SessionStatus.Unknown -> SessionState.Unknown
        is SessionStatus.SignedIn -> SessionState.SignedIn
        is SessionStatus.SignedOut -> SessionState.SignedOut(reason.toDomain())
    }

fun NetworkSignOutReason.toDomain(): SignOutReason =
    when (this) {
        NetworkSignOutReason.NoSession -> SignOutReason.NoSession
        NetworkSignOutReason.LoggedOut -> SignOutReason.LoggedOut
        NetworkSignOutReason.Expired -> SignOutReason.Expired
    }
