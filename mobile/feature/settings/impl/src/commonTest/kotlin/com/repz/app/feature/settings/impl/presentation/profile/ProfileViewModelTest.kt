package com.repz.app.feature.settings.impl.presentation.profile

import com.repz.app.core.domain.model.UnitSystem
import com.repz.app.core.domain.model.User
import com.repz.app.core.domain.model.UserId
import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.domain.user.UserRefreshError
import com.repz.app.core.testing.auth.FakeAuthRepository
import com.repz.app.core.testing.user.FakeUserRepository
import com.repz.app.feature.settings.impl.domain.LogOutUseCase
import com.repz.app.feature.settings.impl.domain.ObserveCurrentUserUseCase
import com.repz.app.feature.settings.impl.domain.RefreshCurrentUserUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

private val FIXTURE_USER =
    User(
        id = UserId(Uuid.parse("00000000-0000-0000-0000-000000000001")),
        email = "alex@repz.app",
        displayName = "Alex",
        unitSystem = UnitSystem.KG,
    )
private val FIXTURE_ACCOUNT = ProfileAccount(displayName = "Alex", email = "alex@repz.app", monogram = "A")

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        userRepository: FakeUserRepository = FakeUserRepository(),
        authRepository: FakeAuthRepository = FakeAuthRepository(),
    ) = ProfileViewModel(
        ObserveCurrentUserUseCase(userRepository),
        RefreshCurrentUserUseCase(userRepository),
        LogOutUseCase(authRepository),
    )

    @Test
    fun givenAStoredUser_whenCreated_thenTheAccountComesFromTheObservedRow() =
        runTest(dispatcher) {
            val viewModel = viewModel(userRepository = FakeUserRepository(initial = FIXTURE_USER))

            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(FIXTURE_ACCOUNT, viewModel.state.value.account)
        }

    @Test
    fun givenNoStoredUser_whenCreated_thenTheAccountIsNullForThePlaceholderHeader() =
        runTest(dispatcher) {
            val viewModel = viewModel(userRepository = FakeUserRepository(initial = null))

            dispatcher.scheduler.advanceUntilIdle()

            assertNull(viewModel.state.value.account)
        }

    @Test
    fun givenANewViewModel_whenCreated_thenRefreshIsCalledOnceOnInit() =
        runTest(dispatcher) {
            val userRepository = FakeUserRepository(initial = FIXTURE_USER)
            viewModel(userRepository = userRepository)

            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(1, userRepository.refreshCallCount)
        }

    @Test
    fun givenARefreshFailure_whenCreated_thenTheObservedStateIsUnaffected() =
        runTest(dispatcher) {
            val userRepository =
                FakeUserRepository(initial = FIXTURE_USER, refreshResult = DomainResult.Failure(UserRefreshError.NoConnection))
            val viewModel = viewModel(userRepository = userRepository)

            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(FIXTURE_ACCOUNT, viewModel.state.value.account)
        }

    @Test
    fun givenTheDialogClosed_whenLogOutClicked_thenTheDialogOpens() =
        runTest(dispatcher) {
            val viewModel = viewModel()

            viewModel.onIntent(ProfileIntent.LogOutClicked)

            assertTrue(viewModel.state.value.isLogOutDialogVisible)
        }

    @Test
    fun givenTheDialogOpen_whenDismissed_thenTheDialogCloses() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            viewModel.onIntent(ProfileIntent.LogOutClicked)

            viewModel.onIntent(ProfileIntent.LogOutDismissed)

            assertEquals(false, viewModel.state.value.isLogOutDialogVisible)
        }

    @Test
    fun givenTheDialogOpen_whenConfirmed_thenTheDialogClosesAndLoggingOutStartsAndLogOutIsCalled() =
        runTest(dispatcher) {
            val authRepository = FakeAuthRepository()
            val viewModel = viewModel(authRepository = authRepository)
            viewModel.onIntent(ProfileIntent.LogOutClicked)

            viewModel.onIntent(ProfileIntent.LogOutConfirmed)
            dispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(false, state.isLogOutDialogVisible)
            assertTrue(state.isLoggingOut)
            assertEquals(1, authRepository.logOutCallCount)
        }

    @Test
    fun givenAlreadyLoggingOut_whenConfirmedAgain_thenLogOutIsCalledOnlyOnce() =
        runTest(dispatcher) {
            val authRepository = FakeAuthRepository()
            val viewModel = viewModel(authRepository = authRepository)
            viewModel.onIntent(ProfileIntent.LogOutClicked)
            viewModel.onIntent(ProfileIntent.LogOutConfirmed)
            dispatcher.scheduler.advanceUntilIdle()

            viewModel.onIntent(ProfileIntent.LogOutConfirmed)
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(1, authRepository.logOutCallCount)
        }

    @Test
    fun givenAlreadyLoggingOut_whenLogOutClickedAgain_thenTheDialogDoesNotReopen() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            viewModel.onIntent(ProfileIntent.LogOutClicked)
            viewModel.onIntent(ProfileIntent.LogOutConfirmed)
            dispatcher.scheduler.advanceUntilIdle()

            viewModel.onIntent(ProfileIntent.LogOutClicked)

            assertEquals(false, viewModel.state.value.isLogOutDialogVisible)
        }

    @Test
    fun givenLoggingOut_whenTheRowEmitsNull_thenTheHeaderIsKeptUntilTheStackSwaps() =
        runTest(dispatcher) {
            val userRepository = FakeUserRepository(initial = FIXTURE_USER)
            val viewModel = viewModel(userRepository = userRepository)
            dispatcher.scheduler.advanceUntilIdle()
            viewModel.onIntent(ProfileIntent.LogOutConfirmed)
            dispatcher.scheduler.advanceUntilIdle()

            userRepository.emit(null)
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(FIXTURE_ACCOUNT, viewModel.state.value.account)
        }
}
