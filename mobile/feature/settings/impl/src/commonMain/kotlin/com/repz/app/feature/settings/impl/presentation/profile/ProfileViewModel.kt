package com.repz.app.feature.settings.impl.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.repz.app.core.domain.model.User
import com.repz.app.feature.settings.impl.domain.LogOutUseCase
import com.repz.app.feature.settings.impl.domain.ObserveCurrentUserUseCase
import com.repz.app.feature.settings.impl.domain.RefreshCurrentUserUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

/**
 * [ProfileIntent.LogOutConfirmed] sets [ProfileUiState.isLoggingOut] and never clears it: the
 * auth gate replaces this screen once the session ends (ADR-0025), the same reasoning as the
 * login screen's `isSubmitting`. While it is set, a `null` row from [ObserveCurrentUserUseCase]
 * (the DAO clear mid-logout) is ignored, so the header never flashes to the empty placeholder
 * before the stack swaps.
 */
@KoinViewModel
class ProfileViewModel(
    observeCurrentUser: ObserveCurrentUserUseCase,
    private val refreshCurrentUser: RefreshCurrentUserUseCase,
    private val logOut: LogOutUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = mutableState.asStateFlow()

    val effects: Flow<ProfileEffect> = emptyFlow()

    init {
        observeCurrentUser()
            .onEach { user ->
                mutableState.update { current ->
                    if (user == null && current.isLoggingOut) {
                        current
                    } else {
                        current.copy(account = user?.toProfileAccount())
                    }
                }
            }.launchIn(viewModelScope)

        viewModelScope.launch { refreshCurrentUser() }
    }

    fun onIntent(intent: ProfileIntent) {
        when (intent) {
            ProfileIntent.LogOutClicked -> onLogOutClicked()
            ProfileIntent.LogOutConfirmed -> onLogOutConfirmed()
            ProfileIntent.LogOutDismissed -> onLogOutDismissed()
        }
    }

    private fun onLogOutClicked() {
        if (mutableState.value.isLoggingOut) return
        mutableState.update { it.copy(isLogOutDialogVisible = true) }
    }

    private fun onLogOutDismissed() {
        mutableState.update { it.copy(isLogOutDialogVisible = false) }
    }

    private fun onLogOutConfirmed() {
        if (mutableState.value.isLoggingOut) return
        mutableState.update { it.copy(isLogOutDialogVisible = false, isLoggingOut = true) }
        viewModelScope.launch { logOut() }
    }
}

private fun User.toProfileAccount() = ProfileAccount(displayName = displayName, email = email, monogram = monogramOf(displayName))
