package com.repz.app.feature.auth.impl.domain

import com.repz.app.core.domain.auth.AuthError
import com.repz.app.core.domain.model.UnitSystem
import com.repz.app.core.domain.model.User
import com.repz.app.core.domain.model.UserId
import com.repz.app.core.domain.model.account.DisplayName
import com.repz.app.core.domain.model.account.EmailAddress
import com.repz.app.core.domain.model.account.Password
import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.testing.auth.FakeAuthRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

private val FIXTURE_EMAIL = (EmailAddress.of("alex@repz.app") as DomainResult.Success).value
private val FIXTURE_PASSWORD = (Password.forRegistration("correct-horse-battery") as DomainResult.Success).value
private val FIXTURE_DISPLAY_NAME = (DisplayName.of("Alex") as DomainResult.Success).value
private val FIXTURE_USER =
    User(
        id = UserId(Uuid.parse("00000000-0000-0000-0000-000000000001")),
        email = FIXTURE_EMAIL.value,
        displayName = FIXTURE_DISPLAY_NAME.value,
        unitSystem = UnitSystem.KG,
    )

class RegisterUseCaseTest {
    @Test
    fun given_successfulRepository_whenInvoked_thenReturnsTheUser() =
        runTest {
            val repository = FakeAuthRepository(registerResult = DomainResult.Success(FIXTURE_USER))
            val useCase = RegisterUseCase(repository)

            val result = useCase(FIXTURE_EMAIL, FIXTURE_PASSWORD, FIXTURE_DISPLAY_NAME)

            assertEquals(DomainResult.Success(FIXTURE_USER), result)
        }

    @Test
    fun given_failingRepository_whenInvoked_thenReturnsTheAuthError() =
        runTest {
            val repository = FakeAuthRepository(registerResult = DomainResult.Failure(AuthError.EmailTaken))
            val useCase = RegisterUseCase(repository)

            val result = useCase(FIXTURE_EMAIL, FIXTURE_PASSWORD, FIXTURE_DISPLAY_NAME)

            assertEquals(DomainResult.Failure(AuthError.EmailTaken), result)
        }

    @Test
    fun given_registrationFields_whenInvoked_thenForwardsThemUnchangedToTheRepository() =
        runTest {
            val repository = FakeAuthRepository()
            val useCase = RegisterUseCase(repository)

            useCase(FIXTURE_EMAIL, FIXTURE_PASSWORD, FIXTURE_DISPLAY_NAME)

            assertEquals(listOf(Triple(FIXTURE_EMAIL, FIXTURE_PASSWORD, FIXTURE_DISPLAY_NAME)), repository.registerCalls)
        }
}
