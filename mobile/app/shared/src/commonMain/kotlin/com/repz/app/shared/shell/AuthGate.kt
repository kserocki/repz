package com.repz.app.shared.shell

import com.repz.app.core.domain.session.SessionRepository
import com.repz.app.core.domain.session.SessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Whether the app shell should show the auth flow or the signed-in shell (ADR-0025). This is a
 * plain app-scoped value, not a screen's ViewModel (Konsist N1 scopes `*ViewModel` to a
 * `feature:x:impl`'s `..presentation..` package): it only drives
 * [com.repz.app.core.navigation.Navigator.syncAuthentication], so it has no intents or effects.
 */
sealed interface AuthGateUiState {
    /** The stored session hasn't been read yet; the shell shows the plain brand background. */
    data object Restoring : AuthGateUiState

    data class Ready(
        val isSignedIn: Boolean,
    ) : AuthGateUiState
}

/**
 * Reads the stored session once at app start and keeps following it afterwards, so a later
 * login, logout or expired refresh moves [state] without a second [SessionRepository.restore]
 * call. [SessionRepository.restore] is idempotent, so a later caller may call it again safely.
 *
 * [applicationScope] is the app-lifetime scope from `CommonModule`: [AuthGate] is a single,
 * provided once for the whole app, not a `ViewModel` (it is read with `koinInject`, never
 * `koinViewModel`, since it has no screen of its own, per Konsist N4).
 */
class AuthGate(
    sessionRepository: SessionRepository,
    applicationScope: CoroutineScope,
) {
    val state: StateFlow<AuthGateUiState> =
        sessionRepository.sessionState
            .map { it.toUiState() }
            .stateIn(applicationScope, SharingStarted.Eagerly, AuthGateUiState.Restoring)

    init {
        applicationScope.launch { sessionRepository.restore() }
    }

    private fun SessionState.toUiState(): AuthGateUiState =
        when (this) {
            SessionState.Unknown -> AuthGateUiState.Restoring
            SessionState.SignedIn -> AuthGateUiState.Ready(isSignedIn = true)
            is SessionState.SignedOut -> AuthGateUiState.Ready(isSignedIn = false)
        }
}
