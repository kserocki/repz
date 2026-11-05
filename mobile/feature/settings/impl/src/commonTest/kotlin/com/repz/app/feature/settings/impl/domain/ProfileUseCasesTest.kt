package com.repz.app.feature.settings.impl.domain

import app.cash.turbine.test
import com.repz.app.core.domain.model.UnitSystem
import com.repz.app.core.domain.model.User
import com.repz.app.core.domain.model.UserId
import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.domain.user.UserRefreshError
import com.repz.app.core.testing.auth.FakeAuthRepository
import com.repz.app.core.testing.user.FakeUserRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

private val FIXTURE_USER =
    User(
        id = UserId(Uuid.parse("00000000-0000-0000-0000-000000000001")),
        email = "alex@repz.app",
        displayName = "Alex",
        unitSystem = UnitSystem.KG,
    )

class ProfileUseCasesTest {
    @Test
    fun givenAStoredUser_whenObserveCurrentUserInvoked_thenForwardsTheRepositorysFlow() =
        runTest {
            val useCase = ObserveCurrentUserUseCase(FakeUserRepository(initial = FIXTURE_USER))

            useCase().test {
                assertEquals(FIXTURE_USER, awaitItem())
            }
        }

    @Test
    fun givenASuccessfulRefresh_whenRefreshCurrentUserInvoked_thenForwardsTheRepositorysResult() =
        runTest {
            val useCase = RefreshCurrentUserUseCase(FakeUserRepository(refreshResult = DomainResult.Success(FIXTURE_USER)))

            val result = useCase()

            assertEquals(DomainResult.Success(FIXTURE_USER), result)
        }

    @Test
    fun givenAFailedRefresh_whenRefreshCurrentUserInvoked_thenForwardsTheRepositorysError() =
        runTest {
            val useCase = RefreshCurrentUserUseCase(FakeUserRepository(refreshResult = DomainResult.Failure(UserRefreshError.NoConnection)))

            val result = useCase()

            assertEquals(DomainResult.Failure(UserRefreshError.NoConnection), result)
        }

    @Test
    fun givenARepository_whenLogOutInvoked_thenCallsTheRepositorysLogOutOnce() =
        runTest {
            val authRepository = FakeAuthRepository()
            val useCase = LogOutUseCase(authRepository)

            useCase()

            assertEquals(1, authRepository.logOutCallCount)
        }
}
