package com.repz.app.shared.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.repz.app.core.domain.session.SessionRepository
import com.repz.app.core.domain.session.SessionState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Whether the app shell should show the auth flow or the signed-in shell. This is not a screen
 * (ADR-0025): it drives [com.repz.app.core.navigation.Navigator.syncAuthentication], so it has no
 * intents or effects, only a state.
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
 * call. [SessionRepository.restore] is idempotent, so a later caller (e.g. REP-26's background
 * sync) may call it again safely.
 */
class AuthGateViewModel(
    sessionRepository: SessionRepository,
) : ViewModel() {
    val state: StateFlow<AuthGateUiState> =
        sessionRepository.sessionState
            .map { it.toUiState() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, AuthGateUiState.Restoring)

    init {
        viewModelScope.launch { sessionRepository.restore() }
    }

    private fun SessionState.toUiState(): AuthGateUiState =
        when (this) {
            SessionState.Unknown -> AuthGateUiState.Restoring
            SessionState.SignedIn -> AuthGateUiState.Ready(isSignedIn = true)
            is SessionState.SignedOut -> AuthGateUiState.Ready(isSignedIn = false)
        }
}
